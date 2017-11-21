/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.composer.ComposerAPI;
import org.hyperledger.composer.ComposerException;

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

public class CompiledQuery<T> extends AbstractQuery<T> {

	private String sql;
	private int numOfParams;

	CompiledQuery(ComposerAPI api, Class<T> clazz, String condition, int conditionOrder, String[] orderFields,
	              int orderOrder, String limit, int limitOrder, String skip, int skipOrder) {
		super(api, clazz);
		int currentVarId = 0, numOfConditionVars = 0;
		List<Pair<Integer, Integer>> varIdPairs = new ArrayList<>();

		if (conditionOrder >= 0) {
			numOfConditionVars = StringUtils.countMatches(condition, '?');
			for (int i = 0; i < numOfConditionVars; i++) {
				varIdPairs.add(Pair.of(conditionOrder, currentVarId++));
			}
		}

		if (limitOrder >= 0 && "?".equals(limit)) {
			varIdPairs.add(Pair.of(limitOrder, currentVarId++));
		}

		if (skipOrder >= 0 && "?".equals(skip)) {
			varIdPairs.add(Pair.of(skipOrder, currentVarId++));
		}

		Collections.sort(varIdPairs);
		int[] varIdReverseMap = new int[varIdPairs.size()];
		for (int i = 0; i < varIdPairs.size(); i++) {
			varIdReverseMap[varIdPairs.get(i).getRight()] = i;
		}

		currentVarId = 0;
		this.sql = MessageFormat.format("SELECT {0}", clazz.getName());
		if (conditionOrder >= 0) {
			while (currentVarId < numOfConditionVars) {
				condition = condition.replaceFirst("\\?",
						MessageFormat.format("_\\$v{0}", varIdReverseMap[currentVarId++]));
			}
			this.sql = MessageFormat.format("{0} WHERE ({1})", this.sql, condition);
		}

		if (orderOrder >= 0) {
			this.sql = MessageFormat.format("{0} ORDER BY {1}", this.sql, Arrays.toString(orderFields));
		}

		if (limitOrder >= 0) {
			if ("?".equals(limit)) {
				this.sql = MessageFormat.format("{0} LIMIT _$v{1}", this.sql, varIdReverseMap[currentVarId++]);
			} else {
				this.sql = MessageFormat.format("{0} LIMIT {1}", this.sql, limit);
			}
		}

		if (skipOrder >= 0) {
			if ("?".equals(skip)) {
				this.sql = MessageFormat.format("{0} SKIP _$v{1}", this.sql, varIdReverseMap[currentVarId++]);
			} else {
				this.sql = MessageFormat.format("{0} SKIP {1}", this.sql, skip);
			}
		}

		this.numOfParams = varIdPairs.size();

	}

	public List<T> execute() throws ComposerException {
		return this.execute("build", this.sql);
	}

	public String getSQL() {
		return this.sql;
	}

	@Override
	protected void validate(ObjectNode params) throws ComposerException {
		Iterator<Entry<String, JsonNode>> iterator = params.fields();
		Set<String> keys = new HashSet<>();
		while (iterator.hasNext()) {
			Entry<String, JsonNode> entry = iterator.next();
			String key = entry.getKey();
			keys.add(key);
		}
		if (keys.contains("v-1")) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "Finding index 0, must start from 1");
		}
		for (int i = 0; i < this.numOfParams; i++) {
			if (!keys.contains(MessageFormat.format("v{0}", i))) {
				throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "Missing parameter " + (i + 1));
			}
		}

		if (keys.size() != this.numOfParams) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "Too many parameters");
		}
	}
}
