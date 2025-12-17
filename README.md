src/

├─ client/

│    ├─ ClientMain.java

│    └─ ClientConnection.java

├─ server/

│    ├─ ServerMain.java

│    ├─ ClientHandler.java

│    └─ GameSession.java

└─ common/

│    ├─ Board.java

│    ├─ Move.java

│    └─ JsonUtil.java

Wzorce:

Singleton – GameSession (jedna gra).

DTO – Move, Board → to co idzie w JSON.

Observer (wersja prymitywna) – serwer powiadamia dwóch klientów o zmianie plans

Instrukcja uruchomienia:

w src\main\java:

javac .\lab4\client\*.java

javac .\lab4\server\*.java

nastęnie odpalamy serwer:

java lab4.server.ServerMain

i odpalamy dwóch klientów(graczy):

java lab4.client.ClientMain

java lab4.client.ClientMain

Opis:

plansza 19×19,

Singleton: GameSession (jedna gra),

DTO (Data Transfer Object): Move, Board (wysyłane w JSON),

Observer (prymitywny): GameSession powiadamia ClientHandler o zmianach planszy,

PASS (pominięcie ruchu),

RESIGN (poddanie się),

zdejmowanie (capturing) grup przeciwnika,

blokada ruchu samobójczego (suicide),

blokada KO (zabronione natychmiastowe powtórzenie pozycji — porównanie z pozycją sprzed ostatniego ruchu),

interfejs konsolowy, który pokazuje planszę i komunikaty,

obsługa błędów i rozłączeń,

Serwer akceptuje dokładnie 2 połączenia i potem uruchamia grę.

Capture: zadziała dla otoczonych grup (rekursywnie / stack).

Po 2x PASS gra się kończy (GAME_OVER Both players passed). Nie ma zaimplementowanego automatycznego liczenia punktów, dodamy później.

Sposób działania komunikacji: (!!!)

Klient → serwer

ClientConnection wysyla np. obiekt Move zamieniony na JSON w JsonUtil; odbiera ClientHandler

Serwer → klient

wysyła ClientHandler (lub GameSession za jego pomocą); odbiera ClientConnection