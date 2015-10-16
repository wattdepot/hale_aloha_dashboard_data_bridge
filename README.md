# hale_aloha_dashboard_data_bridge
Java based data bridge from WattDepot to the Hale Aloha Dashboard

## Installation

The Hale Aloha Data Bridge must be run on the machine that is running the Hale Aloha Dashboard, https://github.com/wattdepot/hale_aloha_dashboard.git.

1. Install Maven, http://maven.apache.org/
2. Clone WattDepot, https://github.com/wattdepot/wattdepot.git
3. Using Maven install WattDepot 3.2.0. This project requires the WattDepot 3.2.0 Client to access the University of Hawaii, Manoa's WattDepot server.
4. Clone the Hale Aloha Data Bridge, https://github.com/wattdepot/hale_aloha_dashboard_data_bridge.git
5. Run `mvn package` in the `hale_aloha_dashboard_data_bridge` directory. This will produce `hale_aloha_dashboard_data_bridge/target/data-bridge-1.0.jar` file.
6. Run `java -cp target/data-bridge-1.0.jar org.wattdepot.dashboard.DataBridgeMain` from the `hale_aloha_dashboard_data_bridge` directory.

## History

2015-10-16: Released version 1.0 of the Data Bridge.

## Credits

Cam Moore
Collaborative Software Development Laboratory, University of Hawaii, Manoa

## License
The MIT License (MIT)

Copyright (c) 2015 CSDL, Cam Moore

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
