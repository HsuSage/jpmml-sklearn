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
package sklearn.compose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jpmml.converter.Feature;
import org.jpmml.python.CastFunction;
import org.jpmml.python.ClassDictUtil;
import org.jpmml.python.HasArray;
import org.jpmml.python.TupleUtil;
import org.jpmml.sklearn.SkLearnEncoder;
import sklearn.Drop;
import sklearn.Initializer;
import sklearn.InitializerUtil;
import sklearn.PassThrough;
import sklearn.Transformer;

public class ColumnTransformer extends Initializer {

	public ColumnTransformer(String module, String name){
		super(module, name);
	}

	@Override
	public List<Feature> initializeFeatures(SkLearnEncoder encoder){
		return encodeFeatures(Collections.emptyList(), encoder);
	}

	@Override
	public List<Feature> encodeFeatures(List<Feature> features, SkLearnEncoder encoder){
		List<Object[]> fittedTransformers = getFittedTransformers();

		List<Feature> result = new ArrayList<>();

		for(Object[] fittedTransformer : fittedTransformers){
			Transformer transformer = getTransformer(fittedTransformer);

			List<Feature> rowFeatures = getFeatures(fittedTransformer, features, encoder);

			rowFeatures = transformer.encode(rowFeatures, encoder);

			result.addAll(rowFeatures);
		}

		return result;
	}

	public List<Object[]> getFittedTransformers(){
		return getTupleList("transformers_");
	}

	static
	private Transformer getTransformer(Object[] fittedTransformer){
		Object transformer = TupleUtil.extractElement(fittedTransformer, 1);

		CastFunction<Transformer> castFunction = new CastFunction<Transformer>(Transformer.class){

			@Override
			public Transformer apply(Object object){

				if(("drop").equals(object)){
					return Drop.INSTANCE;
				} else

				if(("passthrough").equals(object)){
					return PassThrough.INSTANCE;
				}

				return super.apply(object);
			}

			@Override
			protected String formatMessage(Object object){
				return "The estimator object (" + ClassDictUtil.formatClass(object) + ") is not a supported Transformer";
			}
		};

		return castFunction.apply(transformer);
	}

	static
	private List<Feature> getFeatures(Object[] fittedTransformer, List<Feature> features, SkLearnEncoder encoder){
		Object columns = TupleUtil.extractElement(fittedTransformer, 2);

		if((columns instanceof String) || (columns instanceof Integer)){
			columns = Collections.singletonList(columns);
		} else

		if(columns instanceof HasArray){
			HasArray hasArray = (HasArray)columns;

			columns = hasArray.getArrayContent();
		}

		return InitializerUtil.selectFeatures((List)columns, features, encoder);
	}
}