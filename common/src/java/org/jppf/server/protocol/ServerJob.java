/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.Future;

import org.jppf.execute.ExecutorChannel;
import org.jppf.io.DataLocation;
import org.jppf.job.JobEventType;
import org.jppf.job.JobInformation;
import org.jppf.job.JobNotification;
import org.jppf.job.JobNotificationEmitter;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.server.protocol.utils.AbstractServerJob;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.server.submission.SubmissionStatusHandler;
import org.jppf.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Laurent Cohen
 * @exclude
 */
public class ServerJob extends AbstractServerJob
{
  /**
   * State for task indicating whether result or execption was received.
   */
  protected static enum TaskState {
    /**
     * Result was received for task.
     */
    RESULT,
    /**
     * Exception was received for task.
     */
    EXCEPTION
  }

  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJob.class);
  /**
   * The list of of the tasks.
   */
  private final List<DataLocation> tasks;
  /**
   * The list of of the initial tasks.
   */
  private final List<DataLocation> initialTasks;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * Map of all futures in this job.
   */
  private final Map<ServerTaskBundle, Pair<ExecutorChannel, Future>> bundleMap = new LinkedHashMap<ServerTaskBundle, Pair<ExecutorChannel, Future>>();
  /**
   * The status of this submission.
   */
  private SubmissionStatus submissionStatus;
  /**
   * The listener that receives notifications of completed tasks.
   */
  private Object resultsListener;
  /**
   * Instance of parent broadcast job.
   */
  private transient ServerJob parentJob;
  /**
   * Map of all dispatched broadcast jobs.
   */
  private final Map<String, ServerJob> broadcastMap;
  /**
   * Map of all pending broadcast jobs.
   */
  private final Set<ServerJob> broadcastSet = new LinkedHashSet<ServerJob>();
  /**
   * Indicator whether this job is executing.
   */
  private boolean executing = false;
  /**
   * The requeue handler.
   */
  private Runnable onRequeue = null;
  /**
   * State map for tasks on which resultReceived was called.
   */
  private final Map<DataLocation, Pair<DataLocation, TaskState>> taskStateMap = new IdentityHashMap<DataLocation, Pair<DataLocation, TaskState>>();
  /**
   * The location of the data provider.
   */
  private final DataLocation dataProvier;

  private final JobNotificationEmitter notificationEmitter;
  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   */
  public ServerJob(final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider)
  {
    this(notificationEmitter, job, dataProvider, Collections.<DataLocation>emptyList());
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ServerJob(final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider, final List<DataLocation> tasks)
  {
    this(notificationEmitter, job, dataProvider, tasks, null, null);
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID.
   */
  protected ServerJob(final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider, final List<DataLocation> tasks, final ServerJob parentJob, final String broadcastUUID)
  {
    super(job);
    if (tasks == null) throw new IllegalArgumentException("tasks is null");
//    if (notificationEmitter == null) throw new IllegalArgumentException("notificationEmitter is null");

    this.notificationEmitter = notificationEmitter;
    this.dataProvier = dataProvider;
    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;

    if (broadcastUUID == null) {
      if (job.getSLA().isBroadcastJob())
        this.broadcastMap = new LinkedHashMap<String, ServerJob>();
      else
        this.broadcastMap = Collections.emptyMap();
//      this.resultsListener = this.job.getResultListener();
    } else {
      this.broadcastMap = Collections.emptyMap();
      this.resultsListener = null;
    }

//    if (this.job.getResultListener() instanceof SubmissionStatusHandler) this.submissionStatus = ((SubmissionStatusHandler) this.job.getResultListener()).getStatus();
//    else this.submissionStatus = SubmissionStatus.SUBMITTED;
    this.submissionStatus = SubmissionStatus.SUBMITTED;
    this.tasks = new ArrayList<DataLocation>(tasks);
    this.initialTasks = new ArrayList<DataLocation>(tasks);
  }

  public DataLocation getDataProvider() {
    return dataProvier;
  }
  /**
   * Sets indicator whether is job is executing. Job start or job end is notified when state changes.
   * @param executing <code>true</code> when this client job is executing. <code>false</code> otherwise.
   */
  protected void setExecuting(final boolean executing) {
    synchronized (tasks)
    {
      if (this.executing == executing) return;
      this.executing = executing;
    }
//    if (getBroadcastUUID() == null) job.fireJobEvent(executing ? JobEvent.Type.JOB_START: JobEvent.Type.JOB_END);
  }

  /**
   * Get the current number of tasks in the job.
   * @return the number of tasks as an int.
   */
  public int getTaskCount()
  {
    synchronized (tasks)
    {
      return tasks.size();
    }
  }

  /**
   * Get the list of of the tasks.
   * @return a list of <code>DataLocation</code> instances.
   */
  public List<DataLocation> getTasks()
  {
    synchronized (tasks)
    {
      if(getSubmissionStatus() == SubmissionStatus.COMPLETE) {
        List<DataLocation> list = new ArrayList<DataLocation>(initialTasks.size());
        for (DataLocation task : initialTasks) {
          Pair<DataLocation, TaskState> pair = taskStateMap.get(task);
          if(pair != null && pair.second() == TaskState.RESULT)
            list.add(pair.first());
          else
            list.add(task);
        }

        return list;
      } else
        return Collections.unmodifiableList(new ArrayList<DataLocation>(tasks));
    }
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerJob createBroadcastJob(final String broadcastUUID)
  {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");

    ServerJob clientJob;
    synchronized (tasks)
    {
      clientJob = new ServerJob(notificationEmitter, job, dataProvier, this.tasks, this, broadcastUUID);
    }
    synchronized (bundleMap)
    {
      broadcastSet.add(clientJob);
    }
    return clientJob;
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerTaskBundle copy(final int nbTasks)
  {
    JPPFTaskBundle taskBundle = getJob();
    synchronized (tasks)
    {
      if (nbTasks >= this.tasks.size())
      {
        try {
          if(taskBundle.getTaskCount() != this.tasks.size()) {
            taskBundle = taskBundle.copy(this.tasks.size());
          }

          return new ServerTaskBundle(this, taskBundle, this.tasks);
        } finally {
          this.tasks.clear();
        }
      }
      else
      {
        List<DataLocation> subList = this.tasks.subList(0, nbTasks);
        try
        {
          if(taskBundle.getTaskCount() != nbTasks) {
            taskBundle = taskBundle.copy(nbTasks);
          }
          return new ServerTaskBundle(this, taskBundle, subList);
        }
        finally
        {
          subList.clear();
        }
      }
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  public boolean merge(final List<DataLocation> taskList, final boolean after)
  {
    synchronized (tasks)
    {
      boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      if (!after) this.tasks.addAll(0, taskList);
      if (after) this.tasks.addAll(taskList);
      return requeue;
    }
  }

  /**
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public Object getResultListener()
  {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   */
  public void setResultListener(final Object resultsListener)
  {
    this.resultsListener = resultsListener;
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID()
  {
    return broadcastUUID;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundle  the dispatched job.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ServerTaskBundle bundle, final ExecutorChannel channel, final Future<?> future)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (future == null) throw new IllegalArgumentException("future is null");

    boolean empty;
    synchronized (bundleMap)
    {
      empty = bundleMap.isEmpty();
      bundleMap.put(bundle, new Pair<ExecutorChannel, Future>(channel, future));
    }
    if (empty)
    {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
      setExecuting(true);
    }
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ServerTaskBundle bundle, final List<DataLocation> results)
  {
    if (results.isEmpty()) return;

    synchronized (tasks)
    {
      List<DataLocation> bundleTasks = bundle == null ? results : bundle.getTasksL();
      for (int index = 0; index < bundleTasks.size(); index++) {
        DataLocation location = bundleTasks.get(index);
        DataLocation task = results.get(index);
        taskStateMap.put(location, new Pair<DataLocation, TaskState>(task, TaskState.RESULT));
      }
//      for (DataLocation task : results) taskStateMap.put(getPosition(task), TaskState.RESULT);
    }
//    TaskResultListener listener = resultsListener;
//    if (listener != null)
//    {
//      synchronized (listener)
//      {
//        listener.resultsReceived(new TaskResultEvent(results));
//      }
//    }
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ServerTaskBundle bundle, final Throwable throwable)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    synchronized (tasks)
    {
      for (DataLocation task : bundle.getTasksL()) {
        Pair<DataLocation, TaskState> oldPair = taskStateMap.get(task);
        TaskState oldState = oldPair == null ? null : oldPair.second();
        if (oldState != TaskState.RESULT) taskStateMap.put(task, new Pair<DataLocation, TaskState>(null, TaskState.EXCEPTION));
      }
    }
//    TaskResultListener listener = resultsListener;
//    if (listener != null)
//    {
//      synchronized (listener)
//      {
//        listener.resultsReceived(new TaskResultEvent(throwable));
//      }
//    }
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ServerTaskBundle bundle, final Exception exception)
  {
    if (exception != null) {
      exception.printStackTrace(System.out);
    }


    boolean empty;
    synchronized (bundleMap) {
      Pair<ExecutorChannel, Future> pair = bundleMap.remove(bundle);
      Future future = pair == null ? null : pair.second();
      if (bundle != null && future == null) throw new IllegalStateException("future already removed");
      empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
    }
    //if (empty) clearChannels();
    boolean requeue = false;
    if (getSLA().isBroadcastJob()) {
      List<DataLocation> list = new ArrayList<DataLocation>();
      synchronized (tasks)
      {
        if (bundle != null) {
          for (DataLocation task : bundle.getTasksL()) {
            Pair<DataLocation, TaskState> pair = taskStateMap.put(task, new Pair<DataLocation, TaskState>(task, TaskState.RESULT));

            if (pair == null || pair.second() != TaskState.RESULT) list.add(task);
          }
        }
        if (isCancelled() || getBroadcastUUID() == null) {
          list.addAll(this.tasks);
          this.tasks.clear();
        }
      }
      resultsReceived(bundle, list);
    } else if (bundle == null) {
      if (isCancelled()) {
        List<DataLocation> list = new ArrayList<DataLocation>();
        synchronized (tasks)
        {
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
    } else {
      if (bundle.isCancelled()) {
        List<DataLocation> list = new ArrayList<DataLocation>();
        synchronized (tasks)
        {
          for (DataLocation task : bundle.getTasksL()) {
            Pair<DataLocation, TaskState> pair = taskStateMap.get(task);
            if (pair == null || pair.second() != TaskState.RESULT) list.add(task);
          }
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
      if (bundle.isRequeued()) {
        List<DataLocation> list = new ArrayList<DataLocation>();
        synchronized (tasks)
        {
          for (DataLocation task : bundle.getTasksL()) {
            Pair<DataLocation, TaskState> pair = taskStateMap.get(task);
            if (pair != null && pair.second() != TaskState.RESULT) list.add(task);
          }
          requeue = merge(list, false);
        }
      }
    }

    if (hasPending()) {
      if (exception != null) setSubmissionStatus(SubmissionStatus.FAILED);
      if (empty) setExecuting(false);
      if (requeue && onRequeue != null) onRequeue.run();
    } else {
      boolean callDone = updateStatus(EXECUTING, DONE);
      if (empty) setExecuting(false);
      setSubmissionStatus(SubmissionStatus.COMPLETE);
      try {
        if (callDone) done();
      } finally {
        if (parentJob != null)
          parentJob.broadcastCompleted(this);
        else
          getJob().fireTaskCompleted(this);
      }
    }
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some penging tasks.
   */
  protected boolean hasPending() {
    synchronized (tasks)
    {
      if (tasks.isEmpty() && taskStateMap.size() >= job.getTaskCount())
      {
        for (Pair<DataLocation, TaskState> pair : taskStateMap.values())
        {
          if (pair.second() == TaskState.EXCEPTION) return true;
        }
        return false;
      } else return true;
    }
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public SubmissionStatus getSubmissionStatus()
  {
    return submissionStatus;
  }

  /**
   * Set the status of this submission.
   * @param submissionStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus submissionStatus)
  {
    if (this.submissionStatus == submissionStatus) return;
    this.submissionStatus = submissionStatus;
    if (resultsListener instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) resultsListener).setStatus(this.submissionStatus);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    if (super.cancel(mayInterruptIfRunning)) {
      done();
      List<ServerJob> list;
      List<Future>   futureList;
      synchronized (bundleMap)
      {
        list = new ArrayList<ServerJob>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);

        futureList = new ArrayList<Future>(bundleMap.size());
        for (Pair<ExecutorChannel, Future> pair : bundleMap.values()) {
          futureList.add(pair.second());
        }
      }
      for (ServerJob broadcastJob : list) broadcastJob.cancel(mayInterruptIfRunning);
      for (Future future : futureList)
      {
        try
        {
          if (!future.isDone()) future.cancel(false);
        }
        catch (Exception e)
        {
          log.error("Error cancelling job " + this, e);
        }
      }

      boolean empty;
      synchronized (bundleMap)
      {
        broadcastSet.clear();
        empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
      }
      if (empty) taskCompleted(null, null);
      return true;
    }
    else return false;
  }

  /**
   * Called when all or part of broadcast job is dispatched to a driver.
   * @param broadcastJob    the dispatched job.
   */
  protected void broadcastDispatched(final ServerJob broadcastJob)
  {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    boolean empty;
    synchronized (bundleMap)
    {
      broadcastSet.remove(broadcastJob);
      empty = broadcastMap.isEmpty();
      broadcastMap.put(broadcastJob.getBroadcastUUID(), broadcastJob);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
      setExecuting(true);
    }
  }

  /**
   * Called to notify that the execution of broadcasted job has completed.
   * @param broadcastJob    the completed job.
   */
  protected void broadcastCompleted(final ServerJob broadcastJob)
  {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    //    if (debugEnabled) log.debug("received " + n + " tasks for node uuid=" + uuid);
    boolean empty;
    synchronized (bundleMap) {
      if (broadcastMap.remove(broadcastJob.getBroadcastUUID()) != broadcastJob && !broadcastSet.contains(broadcastJob)) throw new IllegalStateException("broadcast job not found");
      empty = broadcastMap.isEmpty();
    }
    if (empty) taskCompleted(null, null);
  }

  /**
   * Set the reuque handler.
   * @param onRequeue {@link Runnable} executed on requeue.
   */
  public void setOnRequeue(final Runnable onRequeue)
  {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be requeud
    this.onRequeue = onRequeue;
  }

  public int getNbChannels() {
    synchronized (bundleMap)
    {
      return bundleMap.size();
    }
  }

  /**
   * A new job was submitted to the JPPF driver queue.
   */
  public void fireJobQueued() {
    fireJobNotification(createJobNotification(JobEventType.JOB_QUEUED, null));
  }

  /**
   * A job was completed and sent back to the client.
   */
  public void fireJobEnded() {
    fireJobNotification(createJobNotification(JobEventType.JOB_ENDED, null));
  }

  /**
   * A sub-job was dispatched to a node.
   */
  public void fireJobDispatched(final ExecutorChannel channel) {
    fireJobNotification(createJobNotification(JobEventType.JOB_DISPATCHED, channel));
  }

  /**
   * A sub-job returned from a node.
   * @param channel
   */
  public void fireJobReturned(final ExecutorChannel channel) {
    fireJobNotification(createJobNotification(JobEventType.JOB_RETURNED, channel));
  }

  /**
   * The current number of tasks in a job was updated.
   */
  public void fireJobUpdated() {
    fireJobNotification(createJobNotification(JobEventType.JOB_UPDATED, null));
  }

  protected void fireJobNotification(final JobNotification event) {
    if (event == null) throw new IllegalArgumentException("event is null");

    if (notificationEmitter != null) notificationEmitter.fireJobEvent(event);
  }

  protected static JobNotification createJobNotification(final JobEventType eventType, final ExecutorChannel channel, final JPPFTaskBundle bundle) {
    JobSLA sla = bundle.getSLA();
    Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
    JobInformation jobInfo = new JobInformation(bundle.getUuid(), bundle.getName(), bundle.getTaskCount(),
            bundle.getInitialTaskCount(), sla.getPriority(), sla.isSuspended(), (pending != null) && pending);
    jobInfo.setMaxNodes(sla.getMaxNodes());
    JPPFManagementInfo nodeInfo = (channel == null) ? null : channel.getManagementInfo();
    JobNotification event = new JobNotification(eventType, jobInfo, nodeInfo, System.currentTimeMillis());
    if (eventType == JobEventType.JOB_UPDATED)
    {
      Integer n = (Integer) bundle.getParameter(BundleParameter.REAL_TASK_COUNT);
      if (n != null) jobInfo.setTaskCount(n);
    }
    return event;
  }

  protected JobNotification createJobNotification(final JobEventType eventType, final ExecutorChannel channel) {
    JobSLA sla = getSLA();
    boolean pending = isPending();
    JobInformation jobInfo = new JobInformation(getUuid(), getName(), getTaskCount(),
            getTaskCount(), sla.getPriority(), sla.isSuspended(), pending);
    jobInfo.setMaxNodes(sla.getMaxNodes());
    JPPFManagementInfo nodeInfo = (channel == null) ? null : channel.getManagementInfo();
    JobNotification event = new JobNotification(eventType, jobInfo, nodeInfo, System.currentTimeMillis());
    if (eventType == JobEventType.JOB_UPDATED)
    {
      Integer n = (Integer) getJob().getParameter(BundleParameter.REAL_TASK_COUNT);
      if (n != null) jobInfo.setTaskCount(n);
    }
    return event;
  }

  @Override
  public void setPending(final boolean pending) {
    boolean oldValue = isPending();
    super.setPending(pending);
    boolean newValue = isPending();
    if (oldValue != newValue) fireJobUpdated();
  }

  public void setSuspended(final boolean suspended) {
    setSuspended(suspended, false);
  }

  public void setSuspended(final boolean suspended, final boolean requeue) {
    JobSLA sla = getJob().getSLA();
    if (sla.isSuspended() == suspended) return;
    sla.setSuspended(suspended);
    fireJobUpdated();
  }

  public void setMaxNodes(final int maxNodes) {
    if (maxNodes <= 0) return;
    getJob().getSLA().setMaxNodes(maxNodes);
    fireJobUpdated();
  }

  @SuppressWarnings("unchecked")
  public NodeJobInformation[] getNodeJobInformation() {
    Map.Entry<ServerTaskBundle, Pair<ExecutorChannel, Future>>[] entries;
    synchronized (bundleMap)
    {
      entries = bundleMap.entrySet().toArray(new Map.Entry[bundleMap.size()]);
    }
    if (entries.length == 0) return NodeJobInformation.EMPTY_ARRAY;

    NodeJobInformation[] result = new NodeJobInformation[entries.length];
    int i = 0;
    for (Map.Entry<ServerTaskBundle, Pair<ExecutorChannel, Future>> entry : entries) {
      {
        ExecutorChannel channel = entry.getValue().first();
        JPPFManagementInfo nodeInfo = channel.getManagementInfo();
        ServerTaskBundle bundle = entry.getKey();
        Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
        JobInformation jobInfo = new JobInformation(getUuid(), bundle.getName(),
                bundle.getTaskCount(), bundle.getInitialTaskCount(), bundle.getSLA().getPriority(),
                bundle.getSLA().isSuspended(), (pending != null) && pending);
        jobInfo.setMaxNodes(bundle.getSLA().getMaxNodes());
        result[i++] = new NodeJobInformation(nodeInfo, jobInfo);
      }
    }
    return result;
  }
}
