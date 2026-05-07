// cdn.jsdelivr.net/gh/... 国内容易抽风
// export const TIEBA_EMOJI_CDN = 'https://cdn.jsdelivr.net/gh/microlong666/tieba_mobile_emotions@master/'

// Finally方案: 自托管
export const TIEBA_EMOJI_CDN =
  'https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/tieba/'

export const tiebaEmoji = (() => {
  const map = { tieba1: TIEBA_EMOJI_CDN + 'image_emoticon.png' }
  for (let i = 2; i <= 124; i++) {
    if (i > 50 && i < 62) continue
    map[`tieba${i}`] = TIEBA_EMOJI_CDN + `image_emoticon${i}.png`
  }
  return map
})()
