## Summary

<!-- Briefly describe what this PR changes and why. -->

## Type of Change

- [ ] Bug fix
- [ ] New feature / UX improvement
- [ ] Built-in theme (`app/src/main/assets/themes/*.json`)
- [ ] Translation (`app/src/main/res/values-*/strings.xml` or docs)
- [ ] Documentation / helper scripts / CI

## Checklist

- [ ] Tested with D-pad remote controls (`OK`, `Hold OK` / `Menu`, and `Back` never exits the launcher)
- [ ] Updated both English (`values/strings.xml`) and Spanish (`values-es/strings.xml`) strings (if UI text changed)
- [ ] Added/modified theme JSON validated with `./gradlew test` (if theming changed)
- [ ] Confirmed **zero** `INTERNET` permission, **zero** `QUERY_ALL_PACKAGES`, and **zero** GMS / Firebase / telemetry dependencies
- [ ] Confirmed no personal IP addresses, device serials, MAC addresses, or maintainer handles were committed
- [ ] Updated documentation (`README.md`, `README.es.md`, `docs/*.md`, `CHANGELOG.md`) if applicable
