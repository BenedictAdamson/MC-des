# MC-des
Multi-threaded Parallel Discrete Event Simulation (PDES) engine

* This module provides a framework for performing simulations of systems on
a shared-memory computer using multiple threads. It is therefore suitable
only for systems that can record a complete snapshot of their state in the
RAM of a practical computer.
* It is for simulating _asynchronous_ systems, for which the times
of events are widely scattered, so advancement of the simulated state using a
global clock is inefficient.
* In common with most PDES algorithms, this framework partly enables
parallel computation by eliminating an explicit global (shared) _event
list_.
* This framework further enables parallel computation by not attempting to
compute and record a single set of _state variables_ at one point in
time. It instead computes and records the full time history of all the state
variables within a time period of interest.
* Unlike other PDES algorithms, the framework does not mandate a
_process-oriented_ methodology, although it can be used in that manner.
Instead, the system is modelled as composed as a set of objects, each of
which has a time-varying state.
* Unlike other PDES algorithms, explicit message passing between
continually running _logical processes_ is not used to perform the
computation. Instead, the computation is broken down into a large set of
_transactions_. Each transaction computes the effect (the state
change) of one event.
* The algorithm of the framework prevents causality violations by recording
(state read) dependency relationships between the transactions. The framework
is _optimistic_: transactions proceed assuming that they will not
violate causality, but are aborted if the dependency information indicates
that the assumption is invalid. Aborted transactions are eventually
rescheduling, to ensure progress eventually occurs. The known dependency
information is used as scheduling hints to reduce the risk of thrashing.
* Unlike PDES algorithms using _logical processes_, the simulation
events (transactions) of this framework may access (read) the states of any
number of objects.
* The framework enforces causality and partly avoids deadlock by requiring
that the time-stamps of states read by a transaction are strictly before the
time-stamp of the state (or states) written by the transaction. Further
deadlock avoidance is done by transparently merging mutually dependent
transactions into one coordinated transaction.
* The framework is _non blocking_: if a thread creates a
transaction that can not be immediately committed (or aborted), the
transaction is recorded and deferred until more work has been done, rather
than have the thread block awaiting completion of the transaction. The
transactions are therefore suitable for computation by a fixed size thread
pool.
* Only the (state read) dependencies between transactions constrain the
order of their computation. Therefore computation of the state histories for
different objects can proceed at different rates, if those objects are
uncoupled (or only loosely coupled) by dependencies.
* The framework reduces the memory required to record all the state
histories by recording only the changes in the object states. It assumes that
the object states will be recorded as collections of immutable objects, so
successive states can reuse (share) references to immutable objects for the
parts of the state that have not changed, to further reduce the memory
required.
</ul>

## License

© Copyright Benedict Adamson 2018.
 
![GPLV3](https://www.gnu.org/graphics/gplv3-with-text-136x68.png)

MC-des is free software: you can redistribute it and/or modify
it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html)
as published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MC-des is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MC-des.  If not, see <https://www.gnu.org/licenses/>.


## Technologies Used

* [Java 10](https://docs.oracle.com/javase/10/)
* Annotations:
    * [JCIP annotations](http://jcip.net/annotations/doc/net/jcip/annotations/package-summary.html)
    * [SpotBugs annotations](https://javadoc.io/doc/com.github.spotbugs/spotbugs-annotations/3.1.8)
* Development environment:
    * [Eclipse IDE](https://www.eclipse.org/ide/)
    * [Jenkins Editor](https://github.com/de-jcup/eclipse-jenkins-editor)
    * [Eclipse Docker Tooling](https://marketplace.eclipse.org/content/eclipse-docker-tooling)
    * [SpotBugs Eclipse plugin](https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin)
* Software configuration management:
     * [Git](https://git-scm.com/)
     * [GitHub](https://github.com)
* Building:
    * [Maven](https://maven.apache.org/)
    * [SpotBugs Maven plugin](https://spotbugs.github.io/spotbugs-maven-plugin/index.html)
    * [Jenkins](https://jenkins.io/)
    * [Docker](https://www.docker.com/)
    * [Ubuntu 18.04](http://releases.ubuntu.com/18.04/)
* Static analysis and testing:
    * [JUnit 5](https://junit.org/junit5/)
    * [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)
    * [Open Test Alliance for the JVM](https://github.com/ota4j-team/opentest4j)
    * [SpotBugs](https://spotbugs.github.io/)
