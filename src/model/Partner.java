
package model;

public class Partner {

    private String partnerId;
    private String partnerName;
    private double commissionPct;
    private double cancellationFeeSharePct;

    public Partner(String partnerId, String partnerName,
                   double commissionPct,
                   double cancellationFeeSharePct) {

        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.commissionPct = commissionPct;
        this.cancellationFeeSharePct = cancellationFeeSharePct;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public double getCommissionPct() {
        return commissionPct;
    }

    public double getCancellationFeeSharePct() {
        return cancellationFeeSharePct;
    }
}
