<?php $currentPage="What's New" ?>
<html>
		<head>
		<title>JPPF What's new
</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, gloud, open source">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
					<?php
		if (!isset($currentPage))
		{
			$currentPage = $_REQUEST["page"];
			if (($currentPage == NULL) || ($currentPage == ""))
			{
				$currentPage = "Home";
			}
		}
		if ($currentPage != "Forums")
		{
		?>
		<div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
		<?php
		}
		?>
		<!--<div class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">-->
			<table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
				<tr style="height: 80px">
					<td width="20"></td>
					<td width="400" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF"/></a></td>
					<td align="right">
						<table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
							<tr>
								<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Home") echo "btn_start.gif"; else echo "btn_active_start.gif"; ?>') repeat-x scroll left bottom; width: 9px"></td>
								<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Home")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/index.php" class="headerMenuItem2">Home</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/index.php" class="headerMenuItem">Home</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "About")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/about.php" class="headerMenuItem2">About</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/about.php" class="headerMenuItem">About</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Download")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/downloads.php" class="headerMenuItem2">Download</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/downloads.php" class="headerMenuItem">Download</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Documentation")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/wiki" class="headerMenuItem2">Documentation</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/wiki" class="headerMenuItem">Documentation</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Forums")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/forums" class="headerMenuItem2">Forums</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/forums" class="headerMenuItem">Forums</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
								<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Forums") echo "btn_end.gif"; else echo "btn_active_end.gif"; ?>') repeat-x scroll right bottom; width: 9px"></td>
							</tr>
						</table>
					</td>
					<td width="20"></td>
				</tr>
			</table>
		<!--</div>-->
			<!--
			<table width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color: white"><tr><td valign="middle" align="center" style="height: 30px; border: 1px solid #6D78B6; border-bottom: none">
				<h4 style="color: #6D78B6; margin-top: 0px; margin-bottom: 0px">The Extensible Grid Computing Platform</h4>
			</td></tr></table>
			-->
					<table border="0" cellspacing="0" cellpadding="5" width="100%px" style="border: 1px solid #6D78B6; border-top: 8px solid #6D78B6;">
			<tr>
				<td style="background-color: #FFFFFF">
				<div class="sidebar">
									<br/>
															<?php
											$itemClass = "";
											if ($currentPage == "Home") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/" class="<?php echo $itemClass; ?>">&raquo; Home</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "About") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="about.php" class="<?php echo $itemClass; ?>">&raquo; About</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Download") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="downloads.php" class="<?php echo $itemClass; ?>">&raquo; Download</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Features") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="features.php" class="<?php echo $itemClass; ?>">&raquo; Features</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/wiki" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br>
											</div>
															<a href="/api-2.0" class="<?php if ($currentPage == Javadoc) echo 'aboutMenuItem'; else echo 'aboutMenuItem2'; ?>">&raquo; Javadoc</a><br>
															<?php
											$itemClass = "";
											if ($currentPage == "Samples") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="../samples-pack/index.html" class="<?php echo $itemClass; ?>">&raquo; Samples</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "License") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br>
											</div>
				<hr/>
															<?php
											$itemClass = "";
											if ($currentPage == "Press") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="release_notes.php?version=2.1" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="screenshots.php" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "News") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br>
											</div>
				<hr/>
															<?php
											$itemClass = "";
											if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Services") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br>
											</div>
				<br/>
				</div>
				<div class="content">
	<div align="justify">
		<h1>What's new in JPPF 2.1</h1>
		<h3>Performance</h3>
		The development of this release has seen a lot of emphasis on both memory usage and performance improvements.<br/>
		The JPPF server now executes multithreaded network I/O and reaches new levels of scalability.<br/>
		JPPF applications can now enjoy faster execution of larger jobs.
		<h3>Localization</h3>
		Russian localization is now available for the graphical administration console.
		<h3>Load balancing</h3>
		Custom load-balancers can now use information about the nodes' environment and configuration, along with metadata about the jobs.<br/>
		This allows for sophisticated load-balancing implementations that can adapt to the capabilities of each node and the computational characteristics of each job.<br/>
		A sophisticated and fully documented <a href="samples-pack/CustomLoadBalancer/Readme.html"><b>sample</b></a> illustrates these new capabilities and will get you started in no time.
		<h3>Configuration</h3>
		The managment port auto-incrementation enables servers and nodes to automatically find port available numbers, making JPPF configuration even easier and removing one the  main configuration hurdles.<br/>
		It is now possible to specify the number of concurrent threads performing I/O in the server.
		<h3>New Samples</h3>
		Three new samples complement our offering in the JPPF samples pack:
		<ul>
			<li><a href="samples-pack/DataDependency/Readme.html">Simulation of large portfolio updates</a></li>
			<li><a href="samples-pack/NodeTray/Readme.html">JPPF node health monitor in the system tray</a></li>
			<li><a href="samples-pack/CustomLoadBalancer/Readme.html">An example of a sophisticated load-balancer implementation</a></li>
		</ul>
		<h3>New Feature Requests</h3>
		<ul>
			<li>2966065 - Adapt the load-balancing to the weight of each job</li>
			<li>2955505 - Give load balancers access to the node's configuration</li>
			<li>2182052 - Simple local node monitoring in system tray</li>
		</ul>
		<h3>Bug Fixes</h3>
		<ul>
			<li>2972979 - Un-connected nodes shouldn't report a valid number of thread</li>
			<li>2969881 - Undefined processing.threads causes execution policy to fail</li>
			<li>2969126 - ClientDataProvider feature not implemented in J2EE connector</li>
			<li>2967151 - Improper exception handling in JPPFBroadcaster</li>
			<li>2962404 - Changing load-balancer settings is done at the wrong time</li>
			<li>2955491 - Connection pool not working whith server discovery disabled</li>
			<li>2953562 - Node does not report results of tasks with timeout</li>
			<li>2933677 - Server thread stuck when serialization fails in node</li>
			<li>2914622 - Local IP addresses should include more than 127.0.0.1</li>
			<li>2907258 - JMX initialization failure causes node to stop working</li>
			<li>2907246 - Remote debugging is only possible on localhost</li>
		</ul>
		<h3></h3>
		<h3></h3>
	</div>
	<br>
				</div>
									</td>
				</tr>
			</table>
				<!--<div align="center" style="width: 100%; border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">-->
		<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
			<tr><td colspan="*" style="height: 10px"></td></tr>
			</tr>
			<tr>
				<td align="center" style="font-size: 9pt; color: #6D78B6">
					<a href="http://sourceforge.net/donate/index.php?group_id=135654"><img src="http://images.sourceforge.net/images/project-support.jpg" width="88" height="32" border="0" alt="Support This Project" /></a>
				</td>
				<td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2010 JPPF.org</td>
				<td align="right">
					<a href="http://www.parallel-matters.com"><img src="/images/pm_logo_tiny.jpg" border="0" alt="Powered by Parallel Matters" /></a>&nbsp;
					<a href="http://sourceforge.net/projects/jppf-project">
						<img src="http://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
							alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
					</a>
				</td>
				<td style="width: 10px"></td>
			</tr>
			<tr><td colspan="*" style="height: 10px"></td></tr>
		</table>
	<!--</div>-->
	<div style="background-color: #E2E4F0; width: 100%;"><img src="/images/frame_bottom.gif" border="0"/></div>
		</div>
		</div>
	</body>
</html>
