package com.dehong.duelofSuits.ui.screens

object RulesContent {

    val english = """
# Duel of Suits

## Goal

Be the first player to get rid of all your cards **after the draw pile is empty**.

---

# Setup

- Use a standard **54-card deck** (52 cards plus 2 Jokers).
- Play with **2–4 players**.
- Deal **8 cards** to each player.
- Place the remaining cards face down to form the **draw pile**.
- Turn the bottom card face up and rotate it 90°. Its suit becomes the **trump suit**. A Joker cannot be the bottom card.
- Randomly choose the first **Attacker**.

---

# Card Strength

Within the same suit, cards rank from highest to lowest:

**A > K > Q > J > 10 > 9 > 8 > 7 > 6 > 5 > 4 > 3 > 2**

- Any **trump card** beats any non-trump card.
- A **Joker** beats any card.
- **Jokers cannot be used to start an attack.**

---

# Turn Overview

Each turn has three roles:

- **Attacker** – starts the attack.
- **Defender** – the player to the Attacker's left.
- **Non-defending Players** – may throw in additional cards.

Each turn follows this sequence:

1. **Attacking** – The Attacker plays 1–4 cards of the same rank.
2. **Defending** – The Defender covers all attacking cards.
3. **Throw-In Phase** – Each non-defender may throw in matching-rank cards or pass.
4. **Defending** – The Defender covers all newly thrown-in cards.
5. Repeat steps 3 and 4 until all non-defenders pass consecutively.

---

# Attacking

The Attacker begins by playing **1 to 4 cards of the same rank**.

Examples: one 8 · two Kings · three 5s · four Queens

Jokers cannot be used to start an attack.

---

# Throw-In Phase

Starting with the player to the Attacker's left and going clockwise, each non-defender may:

- Throw in one or more cards matching the rank of **any card on the table**.
- Pass.

**Throw-In Limit:** Total attacking cards may never exceed the number of cards the Defender had at the start of the turn.

---

# Defending

Each defending card covers exactly one attacking card. A card may be covered by:

- A higher card of the same suit.
- Any trump card (if the attack is not trump).
- A Joker.

Examples: ♣5 → ♣8 · ♥Q → ♥A · ♦A → any trump · any card → Joker

If the Defender cannot or chooses not to cover every card, the **defense fails immediately**.

---

# End of Turn

## Successful Defense

All non-defenders pass and every attacking card is covered:

- Discard all cards on the table.
- The Defender becomes the next Attacker.

## Failed Defense

The Defender takes all cards on the table. The Defender skips their next attack turn. The player to the Defender's left becomes the next Attacker.

---

# Drawing Cards

After each turn, starting with the Attacker, players draw clockwise until they have **8 cards** or the draw pile is empty.

---

# End of Game

Once the draw pile is empty, players no longer draw. The **first player to empty their hand wins**.

---

# Special Rule: Only Jokers

If the draw pile is empty, it is your turn to attack, and your hand contains **only Jokers**:

1. Randomly draw **3 cards** from the discard pile.
2. Add those cards to your hand.
3. Start your attack normally.
""".trimIndent()

    val traditionalChinese = """
# 對花（八張）

## 遊戲目標

當**牌庫用完後**，第一位出完所有手牌的玩家獲勝。

---

# 遊戲準備

- 使用一副標準 **54 張撲克牌**（52 張普通牌加 2 張鬼牌）。
- **2～4 位玩家**遊玩。
- 每位玩家發 **8 張牌**。
- 剩餘的牌面朝下放置，作為**牌庫**。
- 將牌庫最底的一張牌翻開並旋轉 90°，該牌的花色即為本局的**王牌花色**。鬼牌不能作為牌庫底牌。
- 隨機決定第一位**攻擊方**。

---

# 牌力大小

同一花色內，牌力由大到小如下：

**A > K > Q > J > 10 > 9 > 8 > 7 > 6 > 5 > 4 > 3 > 2**

- **任何王牌**都可以壓過任何非王牌。
- **鬼牌**可以壓過任何牌。
- **鬼牌不能作為第一張攻擊牌。**

---

# 回合流程

每個回合有三種角色：

- **攻擊方**：發起本回合的攻擊。
- **防守方**：攻擊方左手邊的玩家。
- **其他玩家**：可協助攻擊方跟牌。

每個回合依照以下順序進行：

1. **攻擊** – 攻擊方打出 1～4 張相同點數的牌。
2. **防守** – 防守方防守桌上所有攻擊牌。
3. **跟牌階段** – 其他玩家依順時針方向輪流跟牌或放棄。
4. **防守** – 防守方防守所有新跟上的牌。
5. 重複步驟 3、4，直到所有其他玩家都連續放棄。

---

# 攻擊

攻擊方回合開始時，可一次打出 **1～4 張相同點數**的牌。

例如：一張 8 · 兩張 K · 三張 5 · 四張 Q

鬼牌不能作為第一張攻擊牌。

---

# 跟牌階段

從攻擊方左手邊的玩家開始，依順時針方向，每位其他玩家可以：

- 打出一張或多張與桌上**任意一張牌點數相同**的牌。
- 放棄跟牌。

**跟牌數量限制：** 整個回合中，桌上所有攻擊牌的總數不得超過防守方在回合開始時的手牌數量。

---

# 防守

每張防守牌只能防守一張攻擊牌。防守方式如下：

- 使用同花色且點數較大的牌。
- 若攻擊牌不是王牌，可使用任意王牌。
- 使用鬼牌。

例如：♣5 → ♣8 · ♥Q → ♥A · ♦A → 任意王牌 · 任何牌 → 鬼牌

如果防守方無法防守所有牌，或選擇不防守，則立即**防守失敗**。

---

# 回合結束

## 防守成功

所有其他玩家都放棄跟牌，且所有攻擊牌皆已成功防守：

- 將桌上所有牌放入棄牌堆。
- 防守方成為下一回合的攻擊方。

## 防守失敗

防守方收走桌上所有牌。防守方失去下一回合的攻擊機會。防守方左手邊的玩家成為下一回合的攻擊方。

---

# 補牌

每回合結束後，由**攻擊方**開始，依順時針方向輪流補牌，直到每位玩家有 **8 張手牌**，或牌庫已用完。

---

# 遊戲結束

當牌庫用完後，玩家不再補牌。**第一位出完所有手牌的玩家立即獲勝。**

---

# 特殊規則：手上只剩鬼牌

如果牌庫已用完、輪到你成為攻擊方，且你的手牌**只剩鬼牌**：

1. 從棄牌堆隨機抽 **3 張牌**。
2. 將這 3 張加入手牌。
3. 然後照常開始攻擊。
""".trimIndent()

    val simplifiedChinese = """
# 对花（八张）

## 游戏目标

当**牌库用完后**，第一位出完所有手牌的玩家获胜。

---

# 游戏准备

- 使用一副标准 **54 张扑克牌**（52 张普通牌加 2 张鬼牌）。
- **2～4 位玩家**游玩。
- 每位玩家发 **8 张牌**。
- 剩余的牌面朝下放置，作为**牌库**。
- 将牌库最底的一张牌翻开并旋转 90°，该牌的花色即为本局的**王牌花色**。鬼牌不能作为牌库底牌。
- 随机决定第一位**攻击方**。

---

# 牌力大小

同一花色内，牌力由大到小如下：

**A > K > Q > J > 10 > 9 > 8 > 7 > 6 > 5 > 4 > 3 > 2**

- **任何王牌**都可以压过任何非王牌。
- **鬼牌**可以压过任何牌。
- **鬼牌不能作为第一张攻击牌。**

---

# 回合流程

每个回合有三种角色：

- **攻击方**：发起本回合的攻击。
- **防守方**：攻击方左手边的玩家。
- **其他玩家**：可协助攻击方跟牌。

每个回合按照以下顺序进行：

1. **攻击** – 攻击方打出 1～4 张相同点数的牌。
2. **防守** – 防守方防守桌上所有攻击牌。
3. **跟牌阶段** – 其他玩家按顺时针方向轮流跟牌或放弃。
4. **防守** – 防守方防守所有新跟上的牌。
5. 重复步骤 3、4，直到所有其他玩家都连续放弃。

---

# 攻击

攻击方回合开始时，可一次打出 **1～4 张相同点数**的牌。

例如：一张 8 · 两张 K · 三张 5 · 四张 Q

鬼牌不能作为第一张攻击牌。

---

# 跟牌阶段

从攻击方左手边的玩家开始，按顺时针方向，每位其他玩家可以：

- 打出一张或多张与桌上**任意一张牌点数相同**的牌。
- 放弃跟牌。

**跟牌数量限制：** 整个回合中，桌上所有攻击牌的总数不得超过防守方在回合开始时的手牌数量。

---

# 防守

每张防守牌只能防守一张攻击牌。防守方式如下：

- 使用同花色且点数更大的牌。
- 若攻击牌不是王牌，可使用任意王牌。
- 使用鬼牌。

例如：♣5 → ♣8 · ♥Q → ♥A · ♦A → 任意王牌 · 任何牌 → 鬼牌

如果防守方无法防守所有牌，或选择不防守，则立即**防守失败**。

---

# 回合结束

## 防守成功

所有其他玩家都放弃跟牌，且所有攻击牌均已成功防守：

- 将桌上所有牌放入弃牌堆。
- 防守方成为下一回合的攻击方。

## 防守失败

防守方收走桌上所有牌。防守方失去下一回合的攻击机会。防守方左手边的玩家成为下一回合的攻击方。

---

# 补牌

每回合结束后，由**攻击方**开始，按顺时针方向轮流补牌，直到每位玩家有 **8 张手牌**，或牌库已用完。

---

# 游戏结束

当牌库用完后，玩家不再补牌。**第一位出完所有手牌的玩家立即获胜。**

---

# 特殊规则：手上只剩鬼牌

如果牌库已用完、轮到你成为攻击方，且你的手牌**只剩鬼牌**：

1. 从弃牌堆随机抽 **3 张牌**。
2. 将这 3 张加入手牌。
3. 然后照常开始攻击。
""".trimIndent()
}
