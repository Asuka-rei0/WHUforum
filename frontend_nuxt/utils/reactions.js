const toCdnUrl = (emoji) => {
  const codepoints = Array.from(emoji)
    .map((c) => c.codePointAt(0).toString(16))
    .join('_')
  // 国外镜像有点小卡 (=ﾟωﾟ)ﾉ, 国内大部分地区访问时会触发 SNI 封锁 / DNS 污染
  // return `https://fonts.gstatic.com/s/e/notoemoji/latest/${codepoints}/emoji.svg`

  // loli.net（即字节系开源社区 mirror，比如 jsDelivr 中国优化节点背后的 CDN 体系）. 不会被墙
  return `https://gstatic.loli.net/s/e/notoemoji/latest/${codepoints}/emoji.svg`
}

export const reactionEmojiMap = {
  LIKE: toCdnUrl('❤️'),
  SMILE: toCdnUrl('😁'),
  DISLIKE: toCdnUrl('👎'),
  RECOMMEND: toCdnUrl('👏'),
  CONGRATULATIONS: toCdnUrl('🎉'),
  ANGRY: toCdnUrl('😡'),
  FLUSHED: toCdnUrl('😳'),
  STAR_STRUCK: toCdnUrl('🤩'),
  ROFL: toCdnUrl('🤣'),
  HOLDING_BACK_TEARS: toCdnUrl('🥹'),
  MIND_BLOWN: toCdnUrl('🤯'),
  POOP: toCdnUrl('💩'),
  CLOWN: toCdnUrl('🤡'),
  SKULL: toCdnUrl('☠️'),
  FIRE: toCdnUrl('🔥'),
  EYES: toCdnUrl('👀'),
  FROWN: toCdnUrl('☹️'),
  HOT: toCdnUrl('🥵'),
  EAGLE: toCdnUrl('🦅'),
  SPIDER: toCdnUrl('🕷️'),
  BAT: toCdnUrl('🦇'),
  CHINA: toCdnUrl('🇨🇳'),
  USA: toCdnUrl('🇺🇸'),
  JAPAN: toCdnUrl('🇯🇵'),
  KOREA: toCdnUrl('🇰🇷'),
}
