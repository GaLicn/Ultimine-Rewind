 # Changelog

## [2.0.0]

### Added / 新增
- 新增自适应恢复材料界面，可显示方块数量、材料种类与完整的所需材料列表，并支持滚动浏览
  - Added a responsive restoration-material screen that displays block count, material types, and the full required-material list with scrolling support.
- 每名玩家现在最多保留最近 5 次连锁采集的撤回记录
  - Each player can now retain up to their 5 most recent Ultimine rewind records.

### Changed / 变更
- 背包中材料充足时，按下 `Ctrl + Z` 会直接执行撤回并自动扣除材料；只有材料不足时才打开材料补充界面
  - When the player has enough materials, pressing `Ctrl + Z` now rewinds immediately and consumes the materials automatically; the material-supply screen only opens when materials are missing.
- 撤回材料统计与消耗改为按物品及其标签精确匹配，提升对带有数据的模组物品的兼容性
  - Rewind material counting and consumption now precisely match items and their tags, improving compatibility with modded items carrying data.
- 撤回快捷键改为原生组合键映射，默认显示并使用 `Ctrl + Z`，且可在控制设置中重新绑定
  - The rewind shortcut now uses a native key-chord mapping, displays and defaults to `Ctrl + Z`, and can be rebound in Controls.
