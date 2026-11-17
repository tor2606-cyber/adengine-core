# Security policy

## Supported versions

The project is in early development and has no releases yet. Security fixes are made on `main`.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report it privately through GitHub instead: go to the repository's **Security** tab and choose
**Report a vulnerability**. Include:

- what is affected and how it can be triggered;
- the impact you expect (data exposure, consent bypass, injection into publisher pages, and so on);
- a proof of concept, if you have one.

You will get a first reply within 7 days. Once a fix is available, the advisory is published and you
are credited unless you ask not to be.

## Scope notes

Issues in these areas are treated as security problems even when no data leaks:

- any path where the tag reads or writes device storage, or sets an identifier, without valid consent;
- any way to see or change another tenant's data;
- open redirects through the click endpoint;
- markup that escapes the ad slot and modifies the publisher page.
