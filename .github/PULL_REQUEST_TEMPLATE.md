## What does this change?

<!-- One or two sentences. Link the issue it closes, if there is one: "Closes #12". -->

## Type

- [ ] Bug fix
- [ ] New feature or option
- [ ] Documentation
- [ ] Internal change (refactor, build, tooling)

## Checklist

- [ ] `mvn -B clean package` builds without warnings.
- [ ] `python tools/validar-recursos.py` reports 0 errors and 0 warnings.
- [ ] The bytecode is still Java 8 (major 52): the plugin has to load on 1.8.
- [ ] Every new player-facing text has a key in **both** `messages_en.yml` and `messages_es.yml`.
- [ ] Every new option is documented with a comment in the YAML file and, if it is visible, in the README.
- [ ] If the artwork changed, the three generators in `docs/assets/` were re-run and the PNGs committed.
- [ ] `CHANGELOG.md` has an entry under the unreleased version.

## Notes for the reviewer

<!-- Anything worth knowing: a decision you took, something you are unsure about, screenshots... -->
