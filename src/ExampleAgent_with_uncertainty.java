import java.util.ArrayList;
import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.BidHistory;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
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
    private HashMap<String, HashMap> opponentsIssueMap = new HashMap<>();
    private HashMap<String, Double> opponentIssueWeights = new HashMap<>();
    private BidHistory bidHistory = new BidHistory();
    private List<Bid> bidOrder;
    private static final int UPDATE_THREESHOLD = 10;
    private int offers_counter = 0;


        @Override
	public void init(NegotiationInfo info) {
	    super.init(info);
	    Domain domain = getDomain();
	    AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
	    factory.estimateUsingBidRanks(userModel.getBidRanking());
	    System.out.println("UTILITY_SPACE");
	    System.out.println(factory.getUtilitySpace());
	    System.out.println("UTILITY_SPACE");
	    AdditiveUtilitySpace additiveUtilitySpace = factory.getUtilitySpace();
	    bidOrder = userModel.getBidRanking().getBidOrder();
	    System.out.println(bidOrder);
	    System.out.println(bidOrder.size());
	    List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
	    int totalPossibleBids = 1;
	    for (Issue issue : issues) {
		System.out.println("Inside issue SDFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
		int issueNumber = issue.getNumber();
		//System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));
		// Assuming that issues are discrete only
		IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
		//EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);
		HashMap<String, Integer> valuesMap = new HashMap<String, Integer>();
		int noOfValues = 0;
		for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
		    // Initializing frequency list to 0 for each option
		    valuesMap.put(valueDiscrete.getValue(), 0);
		    //System.out.println(valueDiscrete.getValue());
		    //System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
		    try {
			//System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
		    } catch(Exception e) {
			//System.out.println("We have an exception");
		    }
		    noOfValues++;
		}
		totalPossibleBids *= noOfValues;
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

    private double johnnyBlackEstimateValue(int noOfOptions, int ranking) {
	noOfOptions = noOfOptions == 0 ? 1 : noOfOptions;
	double el1 = noOfOptions - ranking + 1;
	double el2 = (double) noOfOptions;
	return el1 / el2;
    }

    private double johnnyBlackEstimateIssueWeight(ArrayList<Integer> issueFrequencies, int totalFrequencies) {
	double result = 0;
	for(int frequency : issueFrequencies) {
	    result += Math.pow((double) frequency, 2) / Math.pow((double) totalFrequencies, 2);
	}
	return result;
    }

    private int getSortedIndex(HashMap valuesMap, List<ValueDiscrete> keys, int currentFrequency) {
	int result = 0;
	for(ValueDiscrete valueDiscrete : keys) {
	    String key = valueDiscrete.getValue();
	    int frequency = (int) valuesMap.get(key);
	    if(frequency > currentFrequency) {
		result += 1;
	    }
	}
	return result + 1;
    }

    private void estimateOpponentUtility(HashMap<String, HashMap> issMap, HashMap<String, Double> issWeights) {
	System.out.println("INSIDE THE ESTIMATE OPPONENT UTILITY");
	AdditiveUtilitySpace op = new AdditiveUtilitySpace();
	Domain domain = getDomain();
	AdditiveUtilitySpaceFactory opponentFactory = new AdditiveUtilitySpaceFactory(domain);
	AdditiveUtilitySpace opponentAdditiveUtilitySpace = opponentFactory.getUtilitySpace();
	List<Issue> issues = opponentAdditiveUtilitySpace.getDomain().getIssues();
	for (Issue issue : issues) {
	    EvaluatorDiscrete ed = new EvaluatorDiscrete();
	    String issueKey = issue.getName();
	    System.out.println("THE ISSUE IS");
	    System.out.println(issueKey);
	    System.out.println("The issue weight");
	    System.out.println(issWeights.get(issueKey));
	    HashMap valuesMap = issMap.get(issueKey);
	    IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
	    for(ValueDiscrete vd : issueDiscrete.getValues()) {
		String valueKey = vd.getValue();
		double optionValue = (double) valuesMap.get(valueKey);
		ed.setEvaluationDouble(vd, optionValue);
		System.out.println("The value key is");
		System.out.println(valueKey);
		System.out.println(optionValue);
		try {
		    System.out.println(ed.getEvaluationNotNormalized(vd));
		} catch(Exception e) {

		}
	    }
	    ed.setWeight(issWeights.get(issueKey));
	    op.addEvaluator(issue, ed);
	    System.out.println("ESTIMATED OPPONENT UTILITY FOR THIS ISSUE");
	    System.out.println(ed.getWeight());
	}
	System.out.println("OPPONENT UTILITY CRAP");
	System.out.println(op);
	System.out.println("End opponent utility crap");
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
		lastReceivedOffer = offer.getBid();
		AdditiveUtilitySpace op = new AdditiveUtilitySpace();
		bidHistory.add(new BidDetails(lastReceivedOffer, 0));
		List<Issue> issues = lastReceivedOffer.getIssues();
		double totalEstimatedIssueWeights = 0;
		for (Issue issue : issues) {
		    EvaluatorDiscrete ed = new EvaluatorDiscrete();
		    String issueKey = issue.getName();
		    HashMap valuesMap = issuesMap.get(issueKey);
		    Value opponentsPreference = lastReceivedOffer.getValue(issue.getNumber());
		    valuesMap.put(opponentsPreference, new Integer((int)valuesMap.get(opponentsPreference) + 1));
		    System.out.println(valuesMap);
		    valuesMap.get(lastReceivedOffer.getValue(issue.getNumber()));
		    HashMap<String, Double> opponentOptionsValues = new HashMap<>();
		    if(offers_counter >= UPDATE_THREESHOLD) {
			// Updating the opponents estimated utility values with johnny black technique
			ArrayList<Integer> frequencies = new ArrayList<>();
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			int numberOfOptions = issueDiscrete.getValues().size();
			int totalFrequencies = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
			    String valueKey = valueDiscrete.getValue();
			    int frequency = (int) valuesMap.get(valueKey);
			    double estimatedValue = johnnyBlackEstimateValue(numberOfOptions, getSortedIndex(valuesMap, issueDiscrete.getValues(), frequency));
			    opponentOptionsValues.put(valueKey, estimatedValue);
			    frequencies.add(frequency);
			    totalFrequencies += frequency;
			    ed.setEvaluationDouble(valueDiscrete, estimatedValue);
			}
			opponentsIssueMap.put(issueKey, opponentOptionsValues);
			double estimatedIssueWeight = johnnyBlackEstimateIssueWeight(frequencies, totalFrequencies);
			opponentIssueWeights.put(issueKey, estimatedIssueWeight);
			ed.setWeight(estimatedIssueWeight);
			op.addEvaluator(issue, ed);
			totalEstimatedIssueWeights += estimatedIssueWeight;
		    }
		}
		if(offers_counter >= UPDATE_THREESHOLD) {
		    for(Issue issue : issues) {
			String issueKey = issue.getName();
			// Normalizing opponents' estimated weights
			opponentIssueWeights.put(issueKey, opponentIssueWeights.get(issueKey) / totalEstimatedIssueWeights);
		    }
		    System.out.println("PRINTING OPPONENTS ISSUE WEIGHTS QWERTY");
		    System.out.println(opponentIssueWeights);
		    System.out.println("Printing opponents option values");
		    System.out.println(opponentsIssueMap);
		    estimateOpponentUtility(opponentsIssueMap, opponentIssueWeights);
		    System.out.println("OPPONENT ADDITIVE UTILITY SPACE BEFORE NORMALIZATION");
		    System.out.println(op);
		    op.normalizeWeights();
		    System.out.println("OPPONENT ADDITIVE UTILITY SPACE AFTER NORMALIZATION");
		    System.out.println(op);
		    //                System.out.println(opponentAdditiveUtilitySpace);
		    offers_counter = 0;
		} else {
		    offers_counter += 1;
		}
	    }
	    System.out.println("Printing the bid history");
	    System.out.println(bidHistory.getLastBid());
	    System.out.println(bidHistory.size());
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
