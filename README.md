# Ponzu
Automated Test Generator using model inference and dynamic invariant mining

Model Inference Algorithm: State-Enhanced K-tails (http://people.cs.umass.edu/~brun/pubs/pubs/Krka14fse.pdf)

**PROGRESSION: Laptop with data/vm died during military service and wasn't able to retrieve data (SSD w/ PCI), currently backtracking progress. Need to Get Ponzu running on 64bit MacOS with own yices java bindings. Binding is done, need to integrate and try generating tests.**

### Dependencies:

  - Java 1.8 & Scala 2.6
  - GSON 1.7
  - JUnit (latest)
  - ModBat (http://fmv.jku.at/modbat/)
  - Daikon 5.6.2
  - Yices (Currently ~~working on~~ testing 64-bit Yices Java Language Binding (summer '18 mini-project, without this a VM needs to be used)

### Pipeline
![alt_text](https://raw.githubusercontent.com/nsimsiri/ponzu/master/pipeline.png)
  
 
### Current progress on Tests Generated for JFreeCharts Project
![alt text](https://raw.githubusercontent.com/nsimsiri/ponzu/master/status.png)


