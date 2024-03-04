# **Resilient Anonymous Communication for Everyone (RACE) QR Bootstrap Channel Guide**

## **Table of Contents**
TODO
<br></br>

## **Introduction**
This is a RACE Comms Plugin that provides a physically-local bootstrapping channel based on Wifi Direct communications that enables an existing RACE client to bootstrap a new user by both transmitting the RACE app and plugins as well as facilitating enrollment in the RACE network.
</br>

### **Design Goals**
This channel is intended to help RACE clients get software and configuration files to new clients by using a local network to avoid ISP-level monitoring and detection. It is also meant to be straightforward enough for normal people to use.

### **Security Considerations**
This plugin is a research prototype and has not been the subject of an independent security audit or extensive external testing.

The channel involves starting a Wifi hotspot and passing the credentials to other phones via scanning a QR code. If there are local wifi network monitors they may be able to fingerprint the transaction and detect a bootstrapping event has occurred. There is also no guarantee of safety of the new client if the existing client (the one initiating the bootstrapping) is malicious. E.g. there is no way to guarantee the RACE app software or configuratons provided by the existing client are, in fact, genuine.

<br></br>

## **Scope**
This developer guide covers the  development model, building artifacts, running, and troubleshooting.  It is structured this way to first provide context, associate attributes/features with source files, then guide a developer through the build process.  

</br>

### **Audience**
Technical/developer audience.

### **Environment**
Works on x86 and arm64 Android clients. Uses a Wifi direct connection and QR code display and scan, so it is inapplicable for containerized Linux clients.

### **License**
Licensed under the APACHE 2.0 license, see LICENSE file for more information.

### **Additional Reading**
* [RACE Quickstart Guide](https://github.com/tst-race/race-quickstart/blob/main/README.md)

* [What is RACE: The Longer Story](https://github.com/tst-race/race-docs/blob/main/what-is-race.md)

* [Developer Documentation](https://github.com/tst-race/race-docs/blob/main/RACE%20developer%20guide.md)

* [RIB Documentation](https://github.com/tst-race/race-in-the-box/tree/2.6.0/documentation)

<br></br>

## **Implementation Overview**
This plugin borrows heavily from the [TwoSix Java Comms Plugin](https://github.com/tst-race/race-core/tree/2.6.0/plugin-comms-twosix-java) for its code structure. It functions by starting a Wifi Direct hotspot on the device and placing the bootstrap bundle (artifacts and configuration files provided by the RaceSDK) in a directoy to be served by the local webserver. It then displays a QR code to provide Wifi authentication details to the new user, after the user has joined the network a second QR code is displayed to provide the actual URL to pull the bootstrap bundle from.

___Note:___ this plugin does not work on Android devices that do not support multiple simultaneous network connections. E.g. in a normal wifi-only development context, the device must be able to both be connected to the local Wifi the RIB host and linux containers are running on, _as well as hosting a Wifi hotspot_.

<br></br>

## **How To Build**
Run `build_artifacts_in_docker_image.sh`

</br>

## **How To Run**
Add the following argument to a `deployment create` command:
```
    --comms-channel=bootstrapQrWifi --comms-kit=<kit source for race-qr-bootstrap>
```

</br>
