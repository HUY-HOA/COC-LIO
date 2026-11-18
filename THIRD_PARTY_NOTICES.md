# Third-party notices

## Clash of Clans game data mapping

The identifier-to-name mappings used by COC-LIO are informed by:

- clash-of-clans-data, Copyright (c) 2026 Christopher Pezza
- Repository: https://github.com/chiefpansancolt/clash-of-clans-data
- License: MIT

The dataset is used as a reference for game entity names and data identifiers. COC-LIO loads published PNG artwork directly from the repository's public images directory at runtime; the full Node.js package is not bundled in the APK. These images are cached by the Android image loader after retrieval.

MIT License

Copyright (c) 2026 Christopher Pezza

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## ClashKingAssets supplemental game data and CDN references

The supplemental game-data snapshot in `app/src/main/assets/coc_upgrade/supplemental_catalog.json` is based in part on the static game data from:

- Repository: https://github.com/ClashKingInc/ClashKingAssets
- Snapshot commit: `6da8ce864afe2b4510d3342e1c7374e48d7f5a49`
- Upstream license: GNU General Public License v3.0 (GPL-3.0)
- License text: [licenses/GPL-3.0.txt](licenses/GPL-3.0.txt)

The repository also references artwork hosted by the upstream project's CDN at `https://assets.clashk.ing/`; those image files are fetched remotely and are not bundled as binary image files in this repository. The relevant game-data provenance and snapshot dates are recorded in `app/src/main/assets/coc_upgrade/source.json`.

The GPL-3.0 terms apply to the relevant upstream-derived material. This notice does not relicense third-party material or grant rights to Supercell trademarks or game artwork. Review the upstream license and source notices before redistributing modified versions.

## Clash of Clans marks and game assets

Clash of Clans, Supercell and all related game marks and game artwork belong to their respective owners. COC-LIO is unofficial fan content and is not endorsed by Supercell. Use of Supercell game artwork is subject to the Supercell Fan Content Policy: https://supercell.com/en/fan-content-policy/. The policy limits fan content to non-commercial purposes unless Supercell has expressly approved otherwise, and requires a clear unofficial-content disclaimer.
