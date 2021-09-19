console.log("in import_test_json");
fetch("./test-transactions.csv")
    .then(response => {
        return response.blob();
    })
    .then(data => {
        const dT = new DataTransfer();
        dT.items.add(new File([data], './test-transactions.csv'));
        inp.files = dT.files;
        return data;
    });
