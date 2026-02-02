# Allgemein

Ziel ist es per SFTP heruntergeladenen gezippte JSON Dateien nach Kafka zu übertragen, dabei ist konfigurierbar, welche
JSON Property als Kafka Key verwandt wird. Die Dateien können recht groß werden, daher werden sie vor der Übertragung
mit *gzip* komprimiert.

## Quelle Local

Dateien aus konfiguriertem SFTP Download-Verzeichnis werden gelesen und übertragen. Dateien werden unverändert im
lokalen Verzeichnis belassen.

## Quelle SFTP

WIP - Authentifikation noch nicht ganz fertig mit SSL und Passwort

## Zurücksetzen der Verarbeitung

Löschen der Redis-DB löst eine vollständige Übertragung aller Dateien aus.

Erfolgreich übertragene Dateien werden mit Name und `LAST_MODIFIED` Wert in einer REDIS DB gespeichert. Sollen neue
Dateien übertragen werden, so wird überprüft, ob diese Datei bereist in der Datenbank einen Eintrag hat. Fall dieser
fehlt, wird die Datei geschickt.

# Konfiguration

| Umgebungsvariable                     | Bedeutung                                                                                   |
|---------------------------------------|---------------------------------------------------------------------------------------------|
| SPRING_KAFKA_BOOTSTRAPSERVRERS        | Kafka Broker kommasepariert                                                                 |
| SPRING_KAFKA_SECURITY_PROTOKOLL       | SSL                                                                                         |
| SPRING_DATA_REDIS_HOST                | DNS/IP des Key-Value Stores                                                                 |
| SPRING_DATA_REDIS_POST                | Port des Dienstes                                                                           |
| SPRING_DATA_REDIS_PASSWORD            | Passwort                                                                                    |
| APP_TARGET_TOPIC                      | Kafak Topic in welches SFTP Dateien übertragen werden sollen                                |
| APP_SOURCE_USE                        | `local` oder `SFTP` Quelle der Dateien                                                      |
| APP_SOURCE_LOCALPATH                  | Verzeichnis der zu üertragenden Dateien JSON                                                |
| APP_SOURCE_PROPERTYASID               | Pfad zum Datenfeld, welches als Kafka Key genutzt werden soll                               |
| APP_SOURCE_FILENAMEFILTER             | Regulärer Ausdruck als Filter für zu übetragende JSON Dateien                               |
| APP_SOURCE_POLLSIZE                   | Anzahl der gleichzeitig eingelesenen Dateien                                                |
| APP_SOURCE_FIXED_DELAY                | Zeitabstand (in *ms*) in dem nach neune Datien gesucht werden soll. (z.B. 86400000 = 1 Tag) |
| APP_SOURCE_SFTP_URL                   | SFTP Ziel Adresse                                                                           |
| APP_SOURCE_SFTP_USERNAME              | SFTP Benutzer                                                                               |
| APP_SOURCE_SFTP_PASSWORD              | SFTP Benutzerpasswort                                                                       |
| APP_SOURCE_SFTP_ACCESSKEYCERTLOCATION | SFTP Zugangs SSL Zertifikat                                                                 |
| APP_SOURCE_SFTP_DOWNLOADPATH          | SFTP Quellverzeichnis von dem heruntergeladen werden soll                                   |
