for i in {0..28}
do
	python intra.py --train_id=$i
	python top.py
	python deleteTEST.py
done
