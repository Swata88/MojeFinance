# Moje finance – Android (první vývojová verze)

Android Studio: otevřít složku projektu, synchronizovat Gradle, Build > Build APK(s). Vyžaduje Android SDK 35, JDK 17 a internet pro první stažení závislostí.

## Co funguje
- První spuštění načte seed.json z Excelu: 6 213 transakcí, 1 650 stavů majetku a 24 produktů.
- Přehled výdajů/příjmů/bilance, filtry roku a kategorií, vyhledávání, měsíční sloupcové přehledy.
- Rozkliknutí majetkových kategorií na produkty, měsíční snímky.
- Nové transakce, měsíční stavy, nové a upravované smlouvy.
- Uložení lokálně a ruční export úplné zálohy JSON nebo transakcí CSV (v dialogu lze vybrat Google Drive).
- Zavření klávesnice klepnutím mimo pole, tlačítkem Hotovo, při uložení.

## Známá omezení této verze
- Neobsahuje zatím automatickou zálohu Google Drive ani obnovu JSON v aplikaci.
- CSV export zahrnuje transakce; kompletní majetek a produkty jsou v JSON.
- Úprava/smazání starších transakcí a majetkových stavů, import dalšího Excelu, pokročilé grafy a notifikace expirací jsou plánovány.
- Ukládání je nyní do soukromých preferencí aplikace (pro další verzi vhodná Room databáze).
- Historické transakce z listu bilance jsou načteny s původním označením P/V; majetek do roku 2015 v Kč, od roku 2016 v tisících Kč převedených na Kč.
- Historické měsíční snímky nemusejí být vždy k poslednímu kalendářnímu dni, zachováno přesné datum Excelu.
- Neověřeno kompilací v tomto prostředí; případné chyby buildu pošlete včetně řádků začínajících `e:`.

## Bezpečnost
Soubor seed.json obsahuje reálné finanční záznamy. Nesdílejte projekt veřejně. Čísla smluv zadaná do aplikace jsou citlivá; pro produkční verzi doplnit šifrování a bezpečné zálohování.
