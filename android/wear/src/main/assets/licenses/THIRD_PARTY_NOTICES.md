# Third-party notices and distribution status

This document covers the Korean Android publication candidate based on
`6a2efd5ab46c256c55e6b3bfb12ca338c90075b2` (2.8.6 / 36).
The original Codex Meter MIT license is preserved in LICENSE, including
Copyright (c) 2026 Bennett. This license does not override third-party terms.

## Font redistribution resolved for the 2.8.7 build

The original `sesl_indexscroll_group_font.ttf` and `samsungsharpsans_bold.otf` binaries had differing embedded rights notices. They are excluded from this public snapshot and replaced with unmodified official Pretendard Regular/Bold. Four official Pretendard weights are included for the phone UI under SIL OFL 1.1. Copyright (c) 2021 Kil Hyung-jin and the Reserved Font Name Pretendard are retained with the complete license in `licenses/PRETENDARD_OFL.txt` and APK assets.

The legacy dependency entry names remain compatibility aliases; their contents are Pretendard, not Samsung or Sharp Sans. [Technical details and limitations](docs/FONT_REPLACEMENT.md) and `licenses/PRETENDARD_PROVENANCE.json` identify every changed artifact. Source and final APK checks reject the original font hashes. The complete font license is also included inside both patched AARs. No proprietary font distribution grant is asserted.

## Identified dependencies

| Component | Version / scope | Declared terms and source |
|---|---|---|
| Codex Meter | Android baseline 2.8.6 | MIT, original LICENSE retained; upstream [BenItBuhner/Codex-Meter](https://github.com/BenItBuhner/Codex-Meter) |
| OneUI-Design | `0.9.14+oneui8` | [MIT](https://github.com/tribalfs/oneui-design/blob/main/LICENSE); complete text in `licenses/ONEUI-DESIGN-MIT.txt`. The bundled font is replaced as documented above. |
| oneui-icons | `1.1.0` | [MIT](https://github.com/OneUIProject/oneui-icons/blob/main/LICENSE); complete text in `licenses/ONEUI-ICONS-MIT.txt`. Samsung/One UI marks are not endorsements. |
| SESL AndroidX / SESL Material | Versions preserved in `android/vendor/m2` POMs | [Apache-2.0](https://github.com/tribalfs/sesl-androidx/blob/sesl-androidx-main/LICENSE.txt); complete text in `licenses/SESL-APACHE-2.0.txt`. The bundled font is replaced as documented above. |
| AndroidX / Jetpack | Resolved app and Wear runtime dependencies | Apache-2.0; preserve any module-specific NOTICE and embedded license texts. |
| Google Play Services Wearable | Phone `19.0.0`, Wear `20.0.1` | Google Android SDK terms; this proprietary dependency is not relicensed under MIT. [Official terms](https://developer.android.com/studio/terms). |
| JSON-java | `20250517` | License text from that release in `licenses/JSON-LICENSE.txt`; verify the actual pinned release, not assumptions about historical JSON-java licenses. |
| Guava | Wear `33.6.0-android` | Apache-2.0; transitive annotations/support libraries retain their individual terms. |
| Kotlin runtime, coroutines | Transitive only | Preserve resolved versions and Apache-2.0 notices. |
| Protobuf, annotation support | Transitive only | Preserve each dependency's embedded license; some support components use BSD or MIT terms. |
| Gradle wrapper | Verified official 9.6.1 wrapper | Apache-2.0; tool, not application runtime. |

`licenses/SOURCES.json` records the official sources and SHA-256 of downloaded
license texts. Resolved runtime coordinates (190 external components) and 30 distinct embedded
license/NOTICE texts are recorded in `licenses/RUNTIME_DEPENDENCIES.json`.
The POMs also identify ML Kit terms, Shimmer BSD-2-Clause, and Rikka Refine MIT;
these are transitive dependencies and are not all MIT or Apache-2.0.
Seven POMs without direct license fields were traced to their inherited parent
POM declarations; javax.inject was verified from its pinned sources-JAR header.
Those provenance links are recorded in the inventory. This is a distribution
review; it is not a complete vulnerability assessment or an assurance that
every upstream source repository is authorized to distribute every asset.

## Artwork and branding

The upstream Android resource names include an OpenAI-logo graphic, the
original author's GitHub avatar, and original application icons. They were
preserved as upstream application resources; no new endorsement or rights
claim is made. OpenAI, ChatGPT, Codex, Samsung, Galaxy and One UI marks remain
with their respective owners. The Korean tier badges were added locally as
vector artwork. Branding rights are separate from code licenses.

## Release packaging

Every Release includes original LICENSE, this document and the complete license/NOTICE bundle. Phone/Wear APKs carry the same license texts under `assets/licenses/`. The distribution checks verify actual font and license contents. Code licenses do not confer trademark rights or replace each component's own terms.
