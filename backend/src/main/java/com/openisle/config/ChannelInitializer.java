package com.openisle.config;

import com.openisle.model.MessageConversation;
import com.openisle.repository.MessageConversationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelInitializer implements CommandLineRunner {

  private static final String CHANNEL_AVATAR = "/whu-emblem.webp";
  private static final List<CampusChannelSeed> CAMPUS_CHANNELS = List.of(
    new CampusChannelSeed("2024级计算机学院交流群", "课程、作业、竞赛、求助都可以聊"),
    new CampusChannelSeed("珞珈校园生活圈", "食堂、宿舍、活动、二手和日常分享"),
    new CampusChannelSeed("考研保研互助群", "资料分享、复习打卡、经验交流"),
    new CampusChannelSeed("校园活动搭子群", "讲座、社团、运动、演出结伴")
  );

  private final MessageConversationRepository conversationRepository;

  @Override
  public void run(String... args) {
    List<MessageConversation> channels = conversationRepository.findByChannelTrue();

    if (channels.isEmpty()) {
      CAMPUS_CHANNELS
        .stream()
        .limit(2)
        .forEach(seed -> conversationRepository.save(createChannel(seed)));
      return;
    }

    for (int i = 0; i < channels.size(); i++) {
      MessageConversation channel = channels.get(i);
      CampusChannelSeed seed = CAMPUS_CHANNELS.get(i % CAMPUS_CHANNELS.size());
      channel.setName(seed.name());
      channel.setDescription(seed.description());
      channel.setAvatar(CHANNEL_AVATAR);
      conversationRepository.save(channel);
    }
  }

  private MessageConversation createChannel(CampusChannelSeed seed) {
    MessageConversation channel = new MessageConversation();
    channel.setChannel(true);
    channel.setName(seed.name());
    channel.setDescription(seed.description());
    channel.setAvatar(CHANNEL_AVATAR);
    return channel;
  }

  private record CampusChannelSeed(String name, String description) {}
}
