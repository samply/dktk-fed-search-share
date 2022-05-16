# DKTK Federated Search Share

This service acts as middleware between a Searchbroker and a [Blaze FHIR Server][3] for a DKTK
federated search deployment.

# Usage

## Configuration

### Proxy

If a proxy is needed to access the Searchbroker, please use the following Java properties to
configure it inside the environment variable `JAVA_TOOL_OPTIONS`:

* `-Dhttp.proxyHost`
* `-Dhttp.proxyPort`
* `-Dhttp.proxyPassword`
* `-Dhttp.nonProxyHosts`

## References

The FHIR Implementation Guide, that is the basis of the conversions, can be found [here][1]. The
data elements of the target format can be found [here][2].

## License

Copyright 2022 The Samply Community

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.

[1]: <https://simplifier.net/oncology/>

[2]: <https://mdr.ccp-it.dktk.dkfz.de/view.xhtml?namespace=dktk>

[3]: <https://github.com/samply/blaze>
