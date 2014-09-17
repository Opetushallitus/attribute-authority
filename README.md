# Attribute Authority #

Palvelu jolla voi mapata henkilötunnuksen OID:ksi ja nimeksi. Palvelu toteuttaa SAML2 Attribute Query -rajapinnan.

## SBT-buildi

### Generoi projekti

Eclipseen:

`./sbt eclipse`

... tai IDEAan:

`./sbt 'gen-idea no-sbt-build-module'`

### Yksikkötestit

`./sbt test`

### War-paketointi

`./sbt package`

### Käännä ja käynnistä (aina muutosten yhteydessä automaattisesti) ##

```sh
$ ./sbt
> ~container:start
```

API osoitteessa [http://localhost:8080/](http://localhost:8080/).
