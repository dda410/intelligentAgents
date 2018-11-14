import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * ExampleAgent returns the bid that maximizes its own utility for half of the negotiation session.
 * In the second half, it offers a random bid. It only accepts the bid on the table in this phase,
 * if the utility of the bid is higher than Example Agent's last bid.
 */
public class Navajodonosoras extends AbstractNegotiationParty {
    private final String description = "Navajo_donosoras";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);

      /* List< Bid > bids = userModel.getBidRanking().getBidOrder();

        for (Bid bid : bids) {
            List< Issue > issuesList = bid.getIssues();

            for (Issue issue : issuesList) {
                System.out.println(issue.getName() + ": " + bid.getValue(issue.getNumber()));
            }
        } */

        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();

        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

            // Assuming that issues are discrete only
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                System.out.println(valueDiscrete.getValue());
                System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
                try {
                    System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
    //Implemented by me
    public Bid generateRandomBidWithUtility(double utilityThreshold) {
        Bid randomBid;
        double utility;
        do {
            randomBid = generateRandomBid();
            try {
                utility = utilitySpace.getUtility(randomBid);
            } catch (Exception e)
            {
                utility = 0.0;
            }
        }
        while (utility < utilityThreshold);
        return randomBid;
    }


    /**
     * When this function is called, it is expected that the Party chooses one of the actions from the possible
     * action list and returns an instance of the chosen action.
     *
     * @param list
     * @return
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
        // The time is normalized, so agents need not be
        // concerned with the actual interna
        // First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
        if (time < 0.7) {

            if (lastReceivedOffer !=null
                    && myLastOffer != null
                    && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {
                return new Accept(this.getPartyId(), lastReceivedOffer);
            }
            return new Offer(this.getPartyId(), this.getMaxUtilityBid());

        } else if (time < 0.95) {

            // Offering a random bid with utility threshold
            if (lastReceivedOffer != null
                    && myLastOffer != null
                    && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {
                return new Accept(this.getPartyId(), lastReceivedOffer);

            } else {

                // Offering a random bid with utility threshold
                double my_threshold = 0.7;
                myLastOffer = generateRandomBidWithUtility(my_threshold);
                return new Offer(this.getPartyId(), myLastOffer);
            }

        } else {

            //Accepting anyways
            if (lastReceivedOffer != null
                    && myLastOffer != null
                    && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {
                return new Accept(this.getPartyId(), lastReceivedOffer);

            } else if (lastReceivedOffer != null
                        && myLastOffer != null
                        && this.utilitySpace.getUtility(lastReceivedOffer) > 0.5) {
                return new Accept(this.getPartyId(), lastReceivedOffer);

            } else if (lastReceivedOffer != null) {
                    // Offering a random bid with utility threshold
                    double my_threshold = 0.3;
                    myLastOffer = generateRandomBidWithUtility(my_threshold);
                    return new Offer(this.getPartyId(), myLastOffer);

            } else {

                    return new Accept(this.getPartyId(), lastReceivedOffer);
                }
        }

    }

    /**
     * This method is called to inform the party that another NegotiationParty chose an Action.
     * @param sender
     * @param act
     */
    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);

        if (act instanceof Offer) { // sender is making an offer
            Offer offer = (Offer) act;

            // storing last received offer
            lastReceivedOffer = offer.getBid();
        }
    }

    /**
     * A human-readable description for this party.
     * @return
     */
    @Override
    public String getDescription() {
        return description;
    }

    private Bid getMaxUtilityBid() {
        try {
            return this.utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}