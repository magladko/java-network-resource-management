
						                              $$$$$$\ $$\   $$\ $$$$$$$$\  $$$$$$\  
                               						\_$$  _|$$$\  $$ |$$  _____|$$  __$$\ 
				                              		  $$ |  $$$$\ $$ |$$ |      $$ /  $$ |
				                              		  $$ |  $$ $$\$$ |$$$$$\    $$ |  $$ |
		                              				  $$ |  $$ \$$$$ |$$  __|   $$ |  $$ |
	                              					  $$ |  $$ |\$$$ |$$ |      $$ |  $$ |
	                              					$$$$$$\ $$ | \$$ |$$ |       $$$$$$  |
			                              			\______|\__|  \__|\__|       \______/ 

======================================================================================================================================

 >> kompilacja projektu:
	
	javac AllocationRequest.java
	
>> uruchomienie odbywa się zgodnie z przedstawioną w specyfikacji procedurą, 
   przykładowe skrypt testowe uruchamiające projekt znajdują się w folderze ./GAKKOtestScripts
   (są to pliki pobrane z platformy Gakko)

>> zaimplementowane zostały:
	
	- podłączanie kolejnych węzłów oraz terminacja całości,
	- odbieranie żądań od klienta i wykonanie rezerwacji przy założeniu, że wybrany węzeł kontaktowy posiada wolne zasoby w wymaganej liczności
	- dowolna alokacja zasobów


======================================================================================================================================

$$$$$$\ $$\      $$\ $$$$$$$\  $$\       $$$$$$$$\ $$\      $$\ $$$$$$$$\ $$\   $$\ $$$$$$$$\  $$$$$$\   $$$$$$\     $$$$$\  $$$$$$\  
\_$$  _|$$$\    $$$ |$$  __$$\ $$ |      $$  _____|$$$\    $$$ |$$  _____|$$$\  $$ |\__$$  __|$$  __$$\ $$  __$$\    \__$$ |$$  __$$\ 
  $$ |  $$$$\  $$$$ |$$ |  $$ |$$ |      $$ |      $$$$\  $$$$ |$$ |      $$$$\ $$ |   $$ |   $$ /  $$ |$$ /  \__|      $$ |$$ /  $$ |
  $$ |  $$\$$\$$ $$ |$$$$$$$  |$$ |      $$$$$\    $$\$$\$$ $$ |$$$$$\    $$ $$\$$ |   $$ |   $$$$$$$$ |$$ |            $$ |$$$$$$$$ |
  $$ |  $$ \$$$  $$ |$$  ____/ $$ |      $$  __|   $$ \$$$  $$ |$$  __|   $$ \$$$$ |   $$ |   $$  __$$ |$$ |      $$\   $$ |$$  __$$ |
  $$ |  $$ |\$  /$$ |$$ |      $$ |      $$ |      $$ |\$  /$$ |$$ |      $$ |\$$$ |   $$ |   $$ |  $$ |$$ |  $$\ $$ |  $$ |$$ |  $$ |
$$$$$$\ $$ | \_/ $$ |$$ |      $$$$$$$$\ $$$$$$$$\ $$ | \_/ $$ |$$$$$$$$\ $$ | \$$ |   $$ |   $$ |  $$ |\$$$$$$  |\$$$$$$  |$$ |  $$ |
\______|\__|     \__|\__|      \________|\________|\__|     \__|\________|\__|  \__|   \__|   \__|  \__| \______/  \______/ \__|  \__|
                                                                                                                                      
======================================================================================================================================

Węzły sieciowe (NetworkNode) komunikują się między sobą za pomocą protokołu TCP i są zorganizowane w strukturze takiego drzewa, że pierwszy zainicjowany węzeł będzie jego korzeniem. 
Każdy węzeł standardowo posiada informacje na temat swoich sąsiadów i z nimi prowadzi więszką część komunikacji. 
Wyjątkiem są tutaj informacje możliwe do pozyskania z treści protokołu lub rozesłanie finalnego statusu alokacji do oczekujących węzłów (oraz kontaktowego).

Węzeł oczekujący - węzeł, który zarezerwował zasoby do alokacji. Oczekuje na ostateczny status alokacji, nowe żądania oczekują jako procesy w SingleThreadedExecutorService()

Węzeł kontaktowy - jedyny węzeł komunikujący się z podłączonym klientem. Jeżeli nie zarezerwołał swoich zasobów do alokacji, nie jest wówczas "węzłem oczekującym" i jest gotowy na przetwarzenie nowego żądania.

Standardowo węzły w komunikacji między sobą posługują się portem podanym w argumentach przy inicjalizacji.
Wyjątkiem są "węzły oczekujące" finalnego statusu lokalizacji oraz węzeł kontaktowy (ComNode), które dla statusu alokacji korzystają z dynamicznie przydzielonego portu (listenPort).

Możliwe komunikaty nadawane przez węzły:

>> HELLO -- przesyłany od nowego węzła do węzłą "rodzica", do którego podłączany jest nowy węzeł
	    "rodzic" po otrzymaniu komunikatu, dodaje nowy węzeł do listy węzłów-dzieci

	=================================================================
	|  HELLO <childNodeId>:<childNodeIp>:<childNodePort>        		|
	=================================================================
	
	- childNodeId: identyfikator podłączanego węzła
	- childNodeIp: adres nowego węzła
	- childNodePort: port nowego węzła


>> TERMINATE -- przesyłany do wszystkich sąsiadujących węzłów (jeżeli wciąż istnieją), kończy działanie węzła

	=================================================================
	|		      	               TERMINATE	            			        |
	=================================================================


>> ALLOCATE -- trwające żądanie alokacji przetworzone przez węzeł komunikacji, 
	       używane przy żądaniach wysyłanych między węzłami
	       zakończone jedną pustą linią

	=================================================================
	| ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>           		|
	|***************************************************************|
	| <clientId> <zasób>:<liczność> [<zasób>:liczność]   		        |
	| [<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]	  |
	| [<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]	  |
	| [...]								                                          |
	| <empty line>							                                    |
	=================================================================
	
	ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>:
		- ComNodeIp:   adres węzła komunikacyjnego
		- ComNodePort: port węzła komunikacyjnego
		- listenPort:  port węzła komunikacyjnego otwarty na komunikację odnośnie finalnego statusu żądania
	
	<clientId> <zasób>:<liczność> [<zasób>:liczność]:
		- clientId: identyfikator żądającego klienta
		- zasób: typ żądanego zasobu
		- liczność: liczność żądanego zasobu
	
	[<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]:
		- opcjonalne
		- zasób: typ przygotowanych do alokacji zasobu (lub placeholder dla utrzymania historii odwiedzin, gdy liczność = 0)
		- liczność: liczność przygotowanych do alokacji zasobów
		- ip węzła: adres IP węzła z oczekującymi zasobami lub węzła odwiedzonego (liczność = 0)
		- port węzła: główny port komunikacyjny węzła, inicjowany z argumentów przy inicjalizacji programu
		- listenPort: dla oczekujących węzłów, port nasłuchu statusu żądania


>> ALLOCATED -- status wysyłany w przypadku udanej alokacji węzłom oczekującym oraz 
		węzłowi komunikacyjnemu, który przekazuje go potem klientowi


	=================================================================
	| 			                  ALLOCATED                      		 		|
	|***************************************************************|
	| [<zasób>:<liczność>:<ip węzła>:<port węzła>]            			|
	| [<zasób>:<liczność>:<ip węzła>:<port węzła>]			            |
	| [...]								                                          |
	=================================================================
	
	[<zasób>:<liczność>:<ip węzła>:<port węzła>]:
		- zasób: typ alokowanego zasobu
		- liczność: liczność alokowanego zasobu
		- ip węzła: adres IP węzła alokującego ww. zasób
		- port węzła: główny port komunikacyjny węzła, inicjowany z argumentów przy inicjalizacji programu

>> FAILED -- (1) status przesyłany węzłom oczekującym oraz węzłowi komunikacyjnego oznaczający nieudaną alokację,
		 następstwem jest zwolnienie zarezerwowanych zasobów
	     (2) status wysyłany klientowi przez węzeł komunikacji oznaczający nieudaną alokację żądanych zasobów
	
	(1)
	=================================================================
	| 			                     FAILED	                      	 		|
	|***************************************************************|
	| <clientId>							                                      |
	=================================================================

	<clientId>:
		- clientId: identyfikator klienta, którego dotyczy nieudana alokacja

	(2)
	=================================================================
	| 			                     FAILED                     		 		|
	=================================================================
	
