package com.myweb.website_core.dto;

/**
 * 统计更新消息DTO
 */
public class StatsUpdateMessageDto extends InteractionMessageDto {
    
    private String operationType; // LIKE, UNLIKE, BOOKMARK, UNBOOKMARK, COMMENT
    private int countChange; // 计数变化量，可以是正数或负数
    private String statsType; // LIKE_COUNT, BOOKMARK_COUNT, COMMENT_COUNT

    public StatsUpdateMessageDto() {
        super();
        setMessageType("STATS_UPDATE");
    }

    public StatsUpdateMessageDto(String messageId, Long userId, String username, Long postId, 
                                String operationType, int countChange, String statsType) {
        super(messageId, userId, username, postId, "STATS_UPDATE");
        this.operationType = operationType;
        this.countChange = countChange;
        this.statsType = statsType;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public int getCountChange() {
        return countChange;
    }

    public void setCountChange(int countChange) {
        this.countChange = countChange;
    }

    public String getStatsType() {
        return statsType;
    }

    public void setStatsType(String statsType) {
        this.statsType = statsType;
    }

    @Override
    public String toString() {
        return "StatsUpdateMessageDto{" +
                "operationType='" + operationType + '\'' +
                ", countChange=" + countChange +
                ", statsType='" + statsType + '\'' +
                ", " + super.toString() +
                '}';
    }
}