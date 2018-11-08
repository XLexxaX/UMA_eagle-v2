# Introduction

LIMES is an entity linking system for finding duplicates in two datasets. It implements various approaches like unsupervised, supervised and active genetic learning algorithms
for finding rules that match the entities of a source to a target dataset. LIMES can be used with a graphical user interface, as a command line tool or as a webserver. This repository
is a fork from the original LIMES system (version 1.5.1 - see [Github page](https://github.com/dice-group/LIMES) or [project page](http://aksw.org/Projects/LIMES.html)) and focuses
on the active genetic learning algorithm EAGLE as implemented in the command line tool. In the course of the following text, some modifications to the algorithm are described.
When reading them recall that those descriptions only refer to EAGLE in the command line tool and cannot be used for in the webserver or GUI version. 

# Running the modified LIMES pipeline


Before you can actually run LIMES, a configuration file needs to be written, which contains major settings like the source and target dataset location and some
machine learning parameters. The configuration in LIMES v2 corresponds to the definitions made on the
[Github wiki](http://dice-group.github.io/LIMES/user_manual/configuration_file/) and furthermore extends it with four options:
	- MAXITERATIONS: The number of active genetic learning cycles (do not interchange with the generations-parameter). 
	- RESULT_LOGFILE: The path to a textfile for logging the best found rules and their performances.
	- MAPPINGSFILE: A file for reading all gold-annotations (i.e. the matching entities in NT-format), so that the user doesn't has to give a response actively. 
	- REPETITIONS: The number of repetitions of one single run; for example useful for stabilizing results or calculating standard deviation.
	- SIMPLE_FITNESS: LIMES can calculate an actual (i.e. simple) F1-Score or a pseudo F1-score (see section "fitness score" below). If set to false, the pseudo fitness
	function will be used. This yields better results than the simple F1-score, but must not be used on non-deduplicated dataset. 
	- ELITISM: This parameter sets the number of best-performing individuals (linkage rules), that should survive an evolution.
	- ORIGINAL: If set to true, the original LIMES pipeline as implemented in Limes v1.5.1 on [Github](https://github.com/dice-group/LIMES) will be used. All mentioned 
	parameters won't take effect. 

Running LIMES' active genetic learning module can be achieved in two ways:
1. Importing the project as an "Existing maven project" into eclipse.
In this case, you have to configure the variable "configFiles" in the MultiCaller.java file in the package src.main.java.org.aksw.limes.core.controller and fill the array
with a number of paths to your config files.
2. Running LIMES with the provided .jar-file in the target-folder. 
In this case you need to define one configuration file in the command line with "java -jar limes-core-v2-SNAPSHOT.jar -i PATH\_TO\_YOUR\_CONFIG\_FILE". This way however lacks the 
option to provide multiple configuration files to LIMES with just one call.


# Overview of the modifications to LIMES
### Fitness function
The fitness function for the EAGLE algorithm is a simple F1-score. However, it includes some particularities. The F1-score is computed two ways:
1. An estimation for runtime boosting is employed.
2. The F-score calculation evaluates predictions, for which the algorithm has no ground truth. This can be very beneficial if the source and target datasets are deduplicated, but
leads to deviations from an actual F1-score. That is why the fitness function has been changed such that an actual F1-score can be used. However, this also leads to a deterioration in
the runtime behaviour.

### Training samples
In its original implementation, the EAGLE algorithm asks the user to annotate samples, learn based on these samples and then forgets about it. The learning cycle then starts again. Various tests
have shown, that EAGLE performance can be increased by remembering all the samples queried to the user. Furthermore, LIMES might ask the same question multiple times in its
original implementation. To make it more consistent with the general notion of active learning, the system will ask questions only once in the modified version 2.0.   

### Evolution of populations
The original EAGLE implementation forgets about its current population in each iteration and evolves completely new rules. This represents a random change of the rules, just like
evolutionary algorithms are meant to work. In the modified version, the population is kept in each iteration and xover and mutation are applied to a previous generation to make 
it more consistent with other genetic learning algorithms.

# Modified files:
The following list contains all classes, in which major modifications have been made to LIMES v1.5.1. Those modifications comprise of greater code sections and significantly change the behaviour of the EAGLE algorithm:
- [Eagle.java](src/main/java/org/aksv/limes/core/ml/algorithm/Eagle.java) - The implementation of the active genetic learning, calling various submodules for f-score calculation and evolution.
- [GoldStandardBatchReader.java](src/main/java/org/aksv/limes/core/controller/GoldStandardBatchReader.java) - Simulates the user providing gold-answers; also contains precise (actual) F-score calculation
- [MLPipeline.java](src/main/java/org/aksv/limes/core/ml/algorithm/Eagle.java) - Kind of an overall controller for the execution of the EAGLE algorithm; controls the overall execution of active genetic learning and inquiries to the user.  
- [MultiCaller.java](src/main/java/org/aksv/limes/core/controller/MultiCaller.java) - An upstream class for calling LIMES multiple times with different configurations.
A large number of additional modifications have been made; but those are just minor ones and do not significantly change the functionality of LIMES.
