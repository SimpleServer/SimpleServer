package simpleserver.message;

import simpleserver.Color;
import simpleserver.Group;
import simpleserver.Player;

public class GroupChat extends AbstractChat {
  Group group;

  public GroupChat(Player sender, Group group) {
    super(sender);
    this.group = group;
    chatRoom = group.getName();
  }

  @Override
  public String buildMessage(String message, Player reciever) {
    return "\u00a7" + group.getColor() + super.buildMessage(message, reciever).substring(2);
  }

  @Override
  protected boolean sendToPlayer(Player reciever) {
    return (reciever.getGroupId() == group.getId()) || reciever.equals(sender);
  }

  @Override
  public void noRecieverFound() {
    sender.addTMessage(Color.RED, "Nobody in group %s is online", group.getName());
  }

}
