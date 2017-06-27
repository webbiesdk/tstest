# TSTest
Testing for errors in TypeScript declaration files.  

## Setting up TSTest
Note: If you are running this form the artifact, none of the setup below is needed. 

 - Dependencies: 
    - NodeJS
    - Java 8
    - IntelliJ
    - TypeScript 2.2 `npm install -g typescript@2.2`
    - (istanbul, for coverage tests) `npm install -g istanbul`
 - Getting the submodules `git submodule update --init --remote`
 - Install JavaScript dependencies `npm install`. 
 - Checkout the `v2` branch in the ts-spec-reader submodule.
 - Set up ts-spec-reader: 
    - `cd ts-spec-reader`
    - `npm install`
    - `tsc --module commonjs src/*.ts`
    
## Running TSTest
TSTest is executed through ant scripts. Make sure to navigate a terminal to the root folder of the project 
(the folder that this readme is in), and then simply execute the scripts described in the below sections.  
 
### Running a benchmark
Execute `ant run` to get a list of benchmarks that can be executed.  
Then run `ant run -Dname=X` (replacing `X` with the name of a benchmark) to execute TSTest on that concrete benchmark.

Use `ant run-all` to run TSTest on all 54 benchmarks (and get a report stating how many mismatches was found in each).

### Using TSTest to actually fix a declaration file
If you wish to add another library to TSTest, this can be done by editing the `RunBenchmarks.java` file (these new benchmarks will show up in the list when running `ant run`).

When using TSTest to fix a declaration file, just use the above mentioned `ant run` command.  
It is however a good idea to only look at some of the first errors, as it tends to be easier to find the root cause form the first errors reported by TSTest.  
Fix at most a few errors before running TSTest again with `ant run`, to avoid looking at errors caused by the root-causes you have already fixed. 

If it is difficult to quess what the root cause of an error is, you can run `ant qualitative -Dname=X` (where X is replaced with the name of a benchmark).   
This will find mismatches in that benchmark, and create a small type test script provoking the same error. 

### Running the tests
TSTest comes with a comprehensive test-suite, use `ant test` to execute it. 

### Replicating the data from the paper

There were three research questions in the paper, I will here go through how to replicate the data used in each of the research-questions.
 
WARNING: Most of these take a VERY long time to run, since most execute multiple configurations for all 54 benchmarks.  
Partial results are printed while the scripts are executed, so it is possible to interrupt the execution and still get a feel for how the results look.   
If you intend to run all the experiments untill completion, I recommend to allocate more RAM to the machine, and change the `THREADS` variable in the top of `AutomaticExperiments.java` to `4` (more threads, more memory, for precision of results, use at most `cores-1` threads (where `cores` is the number of cores in your system)).  

#### RQ1 (Qantitative Evaluation):

Generating a table of how many mismatches are found, and how long it took to find them: `ant RQ1.1`  
Testing how many mismatches different configurations of TSTest finds: `ant RQ1.2`  
Getting coverage metrics: `ant RQ1.3`

#### RQ2 (Do the mismatches detected by TStest indicate bugs that developers likely want to fix?): 

The only automated part of this research question is getting a list of type-errors to classify. 

Run `ant qualitative` to get such a list. `ant qualitative` runs an infinite loop which finds a random type mismatch in a random benchmark, 
and outputs the mismatch to the file `errors.txt`.   
It also generates a small type test script which provokes the error (called `minimizedDriverX.js` (where X is a number)),
and places this small type test script in the same folder as the benchmark (benchmarks are located in the `test/benchmarks` folder).  
Generating a small type test script is likely to take a very long time (tending towards infinite in some rare cases), 
it is therefore a good idea to just leave `ant qualitative` running for a while, and possibly restart it if it seems to be stuck (restarting the script is safe, existing content in `errors.txt` will not be deleted).

After a sufficient amount of type errors has been found, all the mismatches listed in `errors.txt` can be manually classified by finding the root cause of each error, here it is very helpful to use the small generated type test scripts. 

The file `manual experiment notes` contains the mismatches used to get the results found in the paper.
 
#### RQ3 (Can TStest find errors that are missed by other tools?): 

There are two parts to this, splitting the found mismatches into "trivial" and "non-trivial".  
By running `ant run-trivial` (in the same fashion as described above in "Running the tests") you can run TSTest on a benchmark, where it behaves as described in the paper, to find which errors in a benchmark are trivially found.  
These outputs can then be used to classify mismatches as "trivial" or not. 

Figuring out is [TSInfer](https://github.com/webbiesdk/tstools) could find the error requires running TSInfer on the library, 
and then manually figuring out if a comparison between the handwritten declaration file and the inferred declaration file could possibly find the error.   