package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(User user) throws Exception {
        if (userMobile.contains(user.getMobile())) {
            throw new Exception("User already exists");
        }else {
            userMobile.add(user.getMobile());
            return "SUCCESS";
        }
    }

    public Group createGroup(List<User> users) {
        if (users.size() == 2) {
            Group group = new Group(users.get(1).getName(), 2);
            groupUserMap.put(group, users);
            groupMessageMap.put(group, new ArrayList<>());
            adminMap.put(group, users.get(0));
            return group;
        }else {
            this.customGroupCount++;
            Group group = new Group("Group " + customGroupCount, users.size());
            groupUserMap.put(group, users);
            groupMessageMap.put(group, new ArrayList<>());
            adminMap.put(group, users.get(0));
            return group;
        }
    }

    public int createMessage(String content) {
        this.messageId++;
        Message message = new Message(messageId, content);
        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        if (!groupUserMap.containsKey(group))throw new Exception("Group does not exist");
        boolean flag = false;
        List<User> userList = groupUserMap.get(group);
        for (User user : userList) {
            if (user.equals(sender)) {
                flag = true;
                break;
            }
        }

        if (flag) {
            senderMap.put(message, sender);
            List<Message> messageList = groupMessageMap.get(group);
            messageList.add(message);
            groupMessageMap.put(group, messageList);
            return messageList.size();
        }else throw new Exception("You are not allowed to send message");
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        if (adminMap.containsKey(group)) {
            if (adminMap.get(group).equals(approver)) {
                List<User> userList = groupUserMap.get(group);
                boolean userfound = false;
                for (User user1 : userList) {
                    if (user1.equals(user)) {
                        userfound = true;
                        break;
                    }
                }
                if (userfound) {
                    adminMap.put(group, user);
                    return "SUCCESS";
                }else throw new Exception("User is not a participant");
            }else throw new Exception("Approver does not have rights");
        }else throw new Exception("Group does not exist");
    }

    public int removeuser(User user) throws Exception {
        boolean userFound = false;
        Group groupOfUser = null;
        for (Group group : groupUserMap.keySet()) {
            List<User> userList = groupUserMap.get(group);
            for (User user1 : userList) {
                if (user1.equals(user)) {
                    if (adminMap.get(group).equals(user))throw new Exception("Cannot remove admin");
                    userFound = true;
                    groupOfUser = group;
                    break;
                }
            }
            if (userFound)break;
        }

        if (userFound) {
            //remove user from groupUser Map
            List<User> userList = groupUserMap.get(groupOfUser);
            userList.remove(user);
            groupUserMap.put(groupOfUser, userList);

            //remove the messages from groupMessage map
            List<Message> messageList = groupMessageMap.get(groupOfUser);
            List<Message> updatedMessages = new ArrayList<>();
            for (Message message : messageList) {
                if (senderMap.get(message).equals(user))continue;
                updatedMessages.add(message);
            }
            groupMessageMap.put(groupOfUser, updatedMessages);

            //remove from sender map
            HashMap<Message, User> updatedSenderMap = new HashMap<>();
            for (Message message : senderMap.keySet()) {
                if (senderMap.get(message).equals(user))continue;
                updatedSenderMap.put(message, senderMap.get(message));
            }
            senderMap = updatedSenderMap;
            return updatedSenderMap.size() + updatedMessages.size() + userList.size();
        }
        throw new Exception("User not found");
    }

    public String findMessage(Date start, Date end, int k) throws Exception {
        List<Message> messageList = new ArrayList<>();
        for (Group group : groupMessageMap.keySet()) {
            messageList.addAll(groupMessageMap.get(group));
        }

        List<Message> updateMessage = new ArrayList<>();
        for (Message message : messageList) {
            if (message.getTimestamp().after(start) && message.getTimestamp().before(end)) {
                updateMessage.add(message);
            }
        }

        if (updateMessage.size() < k)throw new Exception("K is greater than the number of messages");
        Collections.sort(updateMessage, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o2.getTimestamp().compareTo(o1.getTimestamp());
            }
        });
        return updateMessage.get(k - 1).getContent();
    }
}
