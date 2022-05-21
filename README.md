# MC-des

Multi-threaded Parallel Discrete Event Simulation (PDES) engine

* This provides an engine for performing simulations of systems on a shared-memory computer using multiple threads. It
  is therefore suitable only for systems that can record a complete snapshot of their state in the RAM of a practical
  computer.
* It is for simulating _asynchronous_ systems, that have widely scattered times of events, so advancement of the
  simulated state using a global clock is inefficient.
* In common with most PDES algorithms, this engine partly enables parallel computation by eliminating an explicit
  global (shared) _event list_.
* This engine further enables parallel computation by not attempting to compute and record a single set of _state
  variables_ at one point in time. It instead computes and records a time history of all the state variables.
* Unlike other PDES algorithms, the engine does not mandate a
  _process-oriented_ methodology, although it can be used in that manner. Instead, the engine models the system as
  composed as a set of objects, each of which has a time-varying state.
* This uses an _actor model_ for the simulated objects.
  The simulated objects interact only by sending signals to each other.
  Reception of a signal is an event.
  An event may emit further signals.
* The algorithm of the enforces causality and avoids deadlock by requiring that signals have a propagation delay,
  and that the event caused by reception of a signal depends only on the type of signal
  and the state of the receiver at the time the signal was received.
* Computation of the
  state histories for different objects can proceed at different rates, for objects uncoupled (or only loosely
  coupled) by signals.
  If the engine computes a state history too far ahead, it rolls-back work and reschedules computations.
* The engine reduces the memory required to record all the state histories by recording only the changes in the object
  states. It assumes that the object states will be recorded as collections of immutable objects. Successive states then
  can reuse (share) references to immutable objects for unchanged parts of the state, to further reduce the memory
  required.

## License

© Copyright Benedict Adamson 2018,2021-22.

![GPLV3](https://www.gnu.org/graphics/gplv3-with-text-136x68.png)

MC-des is free software: you can redistribute it and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html)
as published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MC-des is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with MC-des. If not,
see <https://www.gnu.org/licenses/>.

## Technologies Used

* [Java 11](https://docs.oracle.com/javase/11/)
* Annotations:
    * [SpotBugs annotations](https://javadoc.io/doc/com.github.spotbugs/spotbugs-annotations)
* Development environment:
    * [IntelliJ IDEA](https://www.jetbrains.com/idea/)
    * previously [Eclipse IDE](https://www.eclipse.org/ide/)
    * previously [Jenkins Editor](https://github.com/de-jcup/eclipse-jenkins-editor)
    * previously [SpotBugs Eclipse plugin](https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin)
* Software configuration management:
    * [Git](https://git-scm.com/)
    * [GitHub](https://github.com)
* Building:
    * [Maven](https://maven.apache.org/)
    * [SpotBugs Maven plugin](https://spotbugs.github.io/spotbugs-maven-plugin/index.html)
    * [Jenkins](https://jenkins.io/)
    * [Ubuntu](http://ubuntu.com)
* Static analysis and testing:
    * [JUnit 5](https://junit.org/junit5/)
    * [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)
    * [Open Test Alliance for the JVM](https://github.com/ota4j-team/opentest4j)
    * [SpotBugs](https://spotbugs.github.io/)
    * My [DBC-assertions](https://github.com/BenedictAdamson/DBC-assertions) library
