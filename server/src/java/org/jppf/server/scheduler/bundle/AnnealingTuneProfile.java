/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server.scheduler.bundle;

import java.util.Random;


/**
 * This class implements the basis of a profile based on simulated annealing 
 * strategy. The possible move from the best known solution get smaller each
 * time it make a move.
 * This strategy let the algorithm explore the universe of bundle size with 
 * a almost known end. Check method getDecreaseRatio about the maximum number
 * of changes.
 *  
 * @author Domingos Creado
 */
public class AnnealingTuneProfile extends AbstractAutoTuneProfile {

	/**
	 * This parameter defines the multiplicity used to define the range available to
	 * random generator, as the maximum.
	 */
	protected float sizeRatioDeviation = 1f;
	/**
	 * This parameter defines how fast does it will stop generating random numbers. 
	 * This is essential to define what is the size of the universe will be explored. 
	 * Greater numbers make the algorithm stop sooner.
	 * Just as example, if the best solution is between 0-100, the following might
	 * occur:
	 * <ul style="list-style-type: none; text-indent: -20px">
	 * <li>1    => 5 max guesses</li>
	 * <li>2    => 2 max guesses</li>
	 * <li>0.5  => 9 max guesses</li>
	 * <li>0.1  => 46 max guesses</li>
	 * <li>0.05 => 96 max guesses</li>
	 * </ul>
	 * This expected number of guesses might not occur if the number of getMaxGuessToStable()
	 * is short.
	 */
	protected float decreaseRatio = 1f;

	/**
	 * Get the multiplicity used to define the range available to
	 * random generator, as the maximum.
	 * @return the multiplicity as a float value.
	 */
	public float getSizeRatioDeviation()
	{
		return sizeRatioDeviation;
	}
	
	/**
	 * Set the multiplicity used to define the range available to
	 * random generator, as the maximum.
	 * @param sizeRatioDeviation the multiplicity as a float value.
	 */
	public void setSizeRatioDeviation(float sizeRatioDeviation)
	{
		this.sizeRatioDeviation = sizeRatioDeviation;
	}

	/**
	 * Get the decrease rate for this profile.
	 * @return the decrease rate as a float value.
	 */
	public float getDecreaseRatio()
	{
		return decreaseRatio;
	}
	
	/**
	 * Set the decrease rate for this profile.
	 * @param decreaseRatio the decrease rate as a float value.
	 */
	public void setDecreaseRatio(float decreaseRatio)
	{
		this.decreaseRatio = decreaseRatio;
	}

	/**
	 * Generate a difference to be applied to the best known bundle size.
	 * @param bestSize the known best size of bundle.
	 * @param collectedSamples the number of samples that were already collected.
	 * @param rnd a pseudo-random number generator.
	 * @return an always positive diff to be applied to bundle size
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#createDiff(int, int, java.util.Random)
	 */
	public int createDiff(int bestSize, int collectedSamples, Random rnd) {
		long max = rnd.nextInt(Math.max(Math.round(bestSize * getSizeRatioDeviation()), 1));
		return (int) expDist(max, collectedSamples);
	}
	
	/**
	 * This method implements the always decreasing policy of the algorithm.
	 * The ratio define how fast this instance will stop generating random
	 * numbers.
	 * The calculation is performed as max * exp(-x * getDecreaseRatio()).
	 * 
	 * @param max the maximum value this algorithm will generate.
	 * @param x a randomly generated bundle size increment.
	 * @return an int value.
	 */
	private double expDist(long max, long x) {
		//return max * Math.pow(Math.E, -x * getDecreaseRatio());
		return max * Math.exp(-x * getDecreaseRatio());
	}

	/**
	 * Make a copy of this profile.
	 * @return a newly created <code>AutoTuneProfile</code> instance.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#copy()
	 */
	public AutoTuneProfile copy()
	{
		AnnealingTuneProfile p = new AnnealingTuneProfile();
		p.minSamplesToAnalyse = minSamplesToAnalyse;
		p.minSamplesToCheckConvergence = minSamplesToCheckConvergence;
		p.maxDeviation = maxDeviation;
		p.maxGuessToStable = maxGuessToStable;
		p.sizeRatioDeviation = sizeRatioDeviation;
		p.decreaseRatio = decreaseRatio;
		return p;
	}
}
