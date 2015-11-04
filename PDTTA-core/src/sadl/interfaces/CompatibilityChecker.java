package sadl.interfaces;

import sadl.models.PTA.PTAState;

public interface CompatibilityChecker {

	public boolean compatible(PTAState firstState, PTAState secondState);
}
