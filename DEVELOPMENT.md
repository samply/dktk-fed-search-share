# Development

## Usage

Ensure you have Java 17 available. Check out this repo and run:

```sh
mvn clean package
docker-compose build
docker-compose up
```

Import data into the Searchbroker DB:

```sql
insert into samply.authtoken (value) values ('foo');
insert into samply.bank (email, authtoken_id) values ('foo@bar.de', 1);
insert into samply.site (name) values ('foo');
insert into samply.bank_site (bank_id, site_id, approved) values (1, 1, true);
insert into samply.user (username, authid) values ('foo', 1);
```

Import data into Blaze:

```sh
blazectl --server http://localhost:8090/fhir upload <dir>
```

Create a request:

```sh
curl 'http://localhost:8088/rest/teiler/requests' -H 'Content-Type: application/xml' -d '<foo></foo>' -vs 2>&1 | grep Location
```

The Location header contains the request identifier (an UUID).

Fetch statistics:

```sh
curl 'http://localhost:8080/requests/3115a0a9-1e32-47ce-867d-1f4f4924990a/stats' -H 'Accept: application/xml'
```

which should output:

```xml
<ns2:queryResultStatistic xmlns:ns2="http://schema.samply.de/osse/QueryResultStatistic">
  <requestId>3115a0a9-1e32-47ce-867d-1f4f4924990a</requestId>
  <numberOfPages>2</numberOfPages>
  <totalSize>80</totalSize>
</ns2:queryResultStatistic>
```

Fetch the first result page:

```sh
curl 'http://localhost:8080/requests/3115a0a9-1e32-47ce-867d-1f4f4924990a/result?page=0' -H 'Accept: application/xml'
```

## Camunda Best Practises

* [Handling Data in Processes](https://camunda.com/best-practices/handling-data-in-processes/)
* [Dealing With Problems and Exceptions](https://camunda.com/best-practices/dealing-with-problems-and-exceptions/)
