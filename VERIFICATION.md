# Verification Notes

The project source was generated and checked in the current sandbox.

Verified here:
- Core settlement calculator compiled with `javac`.
- Settlement smoke tests passed for multiple creditors, deterministic direct transfers, RM100 / 6 cent rounding, zero-cost sessions, and rejection of a non-participant expense payer.
- `admin.js` passed `node --check`.
- `public-session.js` passed `node --check`.
- `pom.xml` parsed as valid XML.
- `render.yaml` parsed as valid YAML.
- No empty project files were found.
- No obvious live secrets were committed.

Environment limitation:
- Maven is not installed in this sandbox and outbound dependency downloads are unavailable.
- Therefore the full Spring Boot Maven compile/test could not be executed here.

Run locally before deployment:

```powershell
mvn clean test
mvn clean package
```

Then configure the environment variables in `README.md` and run:

```powershell
mvn spring-boot:run
```
