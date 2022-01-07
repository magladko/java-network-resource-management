rem Start 1 network node, then allocate the resources using it
start java NetworkNode -ident 1 -tcpport 9000 A:1 
timeout 1 > NUL
start java NetworkNode -ident 1 -tcpport 9001 -gateway localhost:9000 A:1 
timeout 1 > NUL
start java NetworkNode -ident 1 -tcpport 9002 -gateway localhost:9001 A:1 
timeout 1 > NUL
java NetworkClient -gateway localhost:9001 terminate
