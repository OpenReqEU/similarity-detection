package upc.similarity.compareapi.entity;

public class ReqClusterInfo {

    private String cluster;
    private long date;

    public ReqClusterInfo(String cluster, long date) {
        this.cluster = cluster;
        this.date = date;
    }

    public String getCluster() {
        return cluster;
    }

    public long getDate() {
        return date;
    }
}
