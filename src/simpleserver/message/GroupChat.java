package simpleserver.message;

import simpleserver.Color;
import simpleserver.Player;
import simpleserver.config.xml.Group;

public class GroupChat extends AbstractChat {
  Group group;

  public GroupChat(Player sender, Group group) {
    super(sender);
    this.group = group;
    chatRoom = group.name;
  }

  @Override
  public String buildMessage(String message) {
    return "\u00a7" + group.color + super.buildMessage(message).substring(2);
  }

  @Override
  protected boolean sendToPlayer(Player reciever) {
    return (reciever.getGroupId() == group.id) || reciever.equals(sender);
  }

  @Override
  public void noRecieverFound() {
    sender.addTMessage(Color.RED, "Nobody in group %s is online", group.name);
  }

}
