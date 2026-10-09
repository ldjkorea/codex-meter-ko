# Pretendard replacement — 2.8.7

Official source: [orioncactus/pretendard v1.3.9](https://github.com/orioncactus/pretendard/tree/v1.3.9). Regular, Medium, SemiBold and Bold are byte-identical to the official files. `licenses/PRETENDARD_PROVENANCE.json` records download URLs and SHA-256. The font's own names, tables, outlines and format were not altered. App copies retain the Pretendard primary family name. The complete copyright and [SIL OFL 1.1 license](https://raw.githubusercontent.com/orioncactus/pretendard/v1.3.9/LICENSE) accompany source, both patched AARs, APK assets and release notices; the fonts are not relicensed under MIT.

| Dependency entry | Replacement |
|---|---|
| OneUI-Design `res/font/samsungsharpsans_bold.otf` | Official Pretendard Bold OTF |
| SESL indexscroll `assets/sesl_indexscroll_group_font.ttf` | Official Pretendard Regular OTF |

The legacy entry paths are compatibility aliases, not descriptions of the included font. The second `.ttf` path holds an unchanged OpenType CFF (`OTTO`) font. Android supports [TTF and OTF font files](https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml); the inspected indexscroll class calls `Typeface.createFromAsset` at that exact asset path. No filename suffix test was found in that call path. Android's [Typeface implementation](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/master/graphics/java/android/graphics/Typeface.java) builds the font from asset contents with default system fallback. The group-label branch uses U+1F465 U+FE0E (a standard Unicode people symbol); system fallback may render it differently from Samsung's original font. The application does not instantiate SeslIndexScrollView in its own sources/layouts. Actual OEM index-scroll appearance remains untested.

The OneUI resource ID/path is retained so consumers keep resolving it. Every original class and every other existing ZIP entry was compared with the original AAR and remained identical. Only these two font entries and added OFL notices changed. Alias packaging leaves the font bytes and primary name unchanged; it does not create a modified font under the reserved font name. Original Samsung/Sharp Sans bytes are excluded from the public source and APKs and checked by their original SHA-256 hashes, rather than relying on filenames.

The phone theme (including night), explicit XML font references, UI text helpers, chart/wave canvases and phone/lock widget canvases use Pretendard. Weight family: 400/500/600/700; headings use SemiBold, major numbers Bold. Context-free legacy graphics overloads retain an Android system fallback. Wear UI remains unchanged and uses system fonts; both APKs include licenses because they share vendored components.

This verifies source binding, origin, byte content and compilation. It is not a device rendering test. Narrow screens, large system text, dark mode, OEM hosts and Unicode fallback need device inspection. No redistribution permission for the removed proprietary fonts is claimed.
