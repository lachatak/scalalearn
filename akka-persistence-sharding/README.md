How to Run

In SBT, just run docker:publishLocal to create a local docker container.

To run the cluster, run docker-compose up. This will create 3 nodes, a seed and two regular members, called seed, c1, and c2 respectively.

While running, try opening a new terminal and (from the same directory) try things like docker-compose down seed and watch the cluster nodes respond.