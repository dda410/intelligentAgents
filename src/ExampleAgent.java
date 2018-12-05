import java.util.ArrayList;
import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.HashMap;


/**
 * ExampleAgent returns the bid that maximizes its own utility for half of the negotiation session.
 * In the second half, it offers a random bid. It only accepts the bid on the table in this phase,
 * if the utility of the bid is higher than Example Agent's last bid.
 */
public class ExampleAgent extends AbstractNegotiationParty {
    private final String description = "Example Agent";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private HashMap<String, HashMap> issuesMap = new HashMap<>();

        @Override
	public void init(NegotiationInfo info) {
	    super.init(info);
	    AbstractUtilitySpace utilitySpace = info.getUtilitySpace();

	    AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

	    List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
	    int totalPossibleBids = 1;
	    for (Issue issue : issues) {
		System.out.println("Inside issue SDFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
		int issueNumber = issue.getNumber();
		System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

		// Assuming that issues are discrete only
		IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
		EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);
		HashMap<String, Integer> valuesMap = new HashMap<String, Integer>();
		int noOfValues = 0;
		for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
		    valuesMap.put(valueDiscrete.getValue(), 0);
		    System.out.println(valueDiscrete.getValue());
		    //System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
		    try {
			//System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
		    } catch(Exception e) {
			//System.out.println("We have an exception");
		    }
		    noOfValues++;
		}
		totalPossibleBids *= noOfValues;
		System.out.println(valuesMap);
		issuesMap.put(issue.getName(), valuesMap);
	    }
	    System.out.println("All the possible bids:");
	    System.out.println(totalPossibleBids);
	    System.out.println("THE COMPLETE ISSUE MAP");
	    System.out.println(issuesMap);
	}

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
	    // concerned with the actual internal clock.


	    // First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
	    if (time < 0.5) {
		return new Offer(this.getPartyId(), this.getMaxUtilityBid());
	    } else {
		// Accepts the bid on the table in this phase,
		// if the utility of the bid is higher than Example Agent's last bid.
		if (lastReceivedOffer != null
		                        && myLastOffer != null
		    && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {

		    return new Accept(this.getPartyId(), lastReceivedOffer);
		} else {
		    // Offering a random bid
		    return new Offer(this.getPartyId(), this.generateRandomBidWithUtility((double) 1 - time));
		    //                myLastOffer = generateRandomBid();
		    //                return new Offer(this.getPartyId(), myLastOffer);
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
	    System.out.println("INSIDE RECEIVE MESSAGE");
	    System.out.println(issuesMap);
	    super.receiveMessage(sender, act);

	    if (act instanceof Offer) { // sender is making an offer
		Offer offer = (Offer) act;

		// storing last received offer
		lastReceivedOffer = offer.getBid();

		//System.out.println("Received an offer with the following issues");
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
		List<Issue> issues = lastReceivedOffer.getIssues();
		for (Issue issue : issues) {
		    System.out.println("Printing the shit");
		    HashMap valuesMap = issuesMap.get(issue.getName());
		    System.out.println(valuesMap);
		    Value opponentsPreference = lastReceivedOffer.getValue(issue.getNumber());
		    valuesMap.put(opponentsPreference, new Integer((int)valuesMap.get(opponentsPreference) + 1));


		    System.out.println(valuesMap.get(opponentsPreference));
		    valuesMap.get(lastReceivedOffer.getValue(issue.getNumber()));
		    System.out.println(issue.getName());
		    System.out.println(lastReceivedOffer.getValue(issue.getNumber()));
		    //System.out.println(issue.getName());
		    int issueNumber = issue.getNumber();
		    //System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

		    // Assuming that issues are discrete only
		    IssueDiscrete issueDiscrete = (IssueDiscrete) issue;

		    for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
			//System.out.println(valueDiscrete.getValue());
			try {
			} catch(Exception e) {
			    //System.out.println("We have an exception");
			}
		    }
		}
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
