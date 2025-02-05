/*
 * Copyright (c) 2022 Villu Ruusmann
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
package sklearn.linear_model.stochastic_gradient;

import java.util.List;

import com.google.common.collect.Iterables;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.Schema;
import org.jpmml.converter.regression.RegressionModelUtil;
import sklearn.HasDecisionFunctionField;
import sklearn.Regressor;
import sklearn.svm.OneClassSVMUtil;

public class SGDOneClassSVM extends Regressor implements HasDecisionFunctionField {

	public SGDOneClassSVM(String module, String name){
		super(module, name);
	}

	@Override
	public boolean isSupervised(){
		return false;
	}

	@Override
	public RegressionModel encodeModel(Schema schema){
		List<? extends Number> coef = getCoef();
		List<? extends Number> offset = getOffset();

		RegressionModel regressionModel = RegressionModelUtil.createRegression(schema.getFeatures(), coef, -1 * (Iterables.getOnlyElement(offset).doubleValue()), null, schema)
			.setOutput(OneClassSVMUtil.createPredictedOutput(this));

		return regressionModel;
	}

	public List<? extends Number> getCoef(){
		return getNumberArray("coef_");
	}

	public List<? extends Number> getOffset(){
		return getNumberArray("offset_");
	}
}