/*
 * Copyright (c) 2019 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn2pmml.preprocessing;

import java.util.Collections;
import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.jpmml.converter.BooleanFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.python.ClassDictUtil;
import org.jpmml.sklearn.SkLearnEncoder;

public class MatchesTransformer extends PatternTransformer {

	public MatchesTransformer(String module, String name){
		super(module, name);
	}

	@Override
	public List<Feature> encodeFeatures(List<Feature> features, SkLearnEncoder encoder){
		String pattern = getPattern();

		ClassDictUtil.checkSize(1, features);

		Feature feature = features.get(0);

		if(feature.getDataType() != DataType.STRING){
			throw new IllegalArgumentException();
		}

		Apply apply = PMMLUtil.createApply(PMMLFunctions.MATCHES, feature.ref(), PMMLUtil.createConstant(pattern, DataType.STRING));

		DerivedField derivedField = encoder.createDerivedField(createFieldName("matches", feature, formatArg(pattern)), OpType.CATEGORICAL, DataType.BOOLEAN, apply);

		return Collections.singletonList(new BooleanFeature(encoder, derivedField));
	}
}