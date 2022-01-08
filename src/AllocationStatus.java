public enum AllocationStatus {
    ALLOCATE("ALLOCATE"), ALLOCATED("ALLOCATED"), FAILED("FAILED");

    private String status;

    AllocationStatus(String status) {
        this.status = status;
    }

    public String getStatusString() {
        return status;
    }
}
