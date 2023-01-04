# Security Policy

## Supported Versions

Only the two latest major version are going to receive the latest security patches. However, ways to upgrade will be provided for direct previous versions, 
in case of a critical security issue that can affect the infrastructure of a project.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| 0.3.x   | :white_check_mark: |
| < 0.3   | :x:                |

### Example Scenario

ACME is relying on pottery to build their Java systems, and they are using pottery 0.2.0. Due to a bug in pottery 0.2.0, there is a critical vulnerability 
that allows remote code execution by exploding one of the provided pottery commands. The latest two major versions of pottery are 1.0.0 and 0.3.0, so 0.2.0 is
outside of support.

In that situation, we will release documentation or any other utility that would allow ACME to upgrade their systems to, at least, use 0.3.0, encouraging also ACME
to eventually upgrade to 1.0.0.

## Reporting a Vulnerability

Published CVEs can be reported using GitHub Issues. A template is already provided when creating issues. Unpublished CVEs can be reported to the maintainers of the 
project by email.
