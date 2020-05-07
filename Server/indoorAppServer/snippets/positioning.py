from sklearn.neighbors import KNeighborsClassifier

def apply_knn(training_data,target_values,n_neighbors=5,weight='uniform', algorithm='auto',distance_metric='manhattan'):
    neigh = KNeighborsClassifier(n_neighbors=n_neighbors,weights=weight,algorithm=algorithm,metric=distance_metric)
    neigh.fit(training_data,target_values)