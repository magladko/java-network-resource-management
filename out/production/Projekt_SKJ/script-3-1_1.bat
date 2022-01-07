rem Start 3 network nodes, then allocate the resources using the first one as a gateway
start java NetworkNode -ident 1 -tcpport 9000 A:1 
timeout 1 > NUL
start java NetworkNode -ident 2 -tcpport 9001 -gateway localhost:9000 B:1 
timeout 1 > NUL
start java NetworkNode -ident 3 -tcpport 9002 -gateway localhost:9001 C:1 
timeout 1 > NUL
java NetworkClient -ident 1 -gateway localhost:9000 A:1 B:1 C:1
java NetworkClient -gateway localhost:9000 terminate
