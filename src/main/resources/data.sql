-- Seed location data: Indian States, Cities, and Pincodes
-- Uses MERGE to be idempotent (safe to re-run)

-- Andhra Pradesh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Visakhapatnam', '530001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Visakhapatnam', '530002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Vijayawada', '520001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Vijayawada', '520002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Guntur', '522001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Tirupati', '517501');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Nellore', '524001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Kakinada', '533001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Rajahmundry', '533101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Kurnool', '518001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andhra Pradesh', 'Anantapur', '515001');

-- Arunachal Pradesh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Arunachal Pradesh', 'Itanagar', '791111');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Arunachal Pradesh', 'Naharlagun', '791110');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Arunachal Pradesh', 'Pasighat', '791102');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Arunachal Pradesh', 'Tawang', '790104');

-- Assam
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Assam', 'Guwahati', '781001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Assam', 'Guwahati', '781003');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Assam', 'Silchar', '788001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Assam', 'Dibrugarh', '786001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Assam', 'Jorhat', '785001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Assam', 'Tezpur', '784001');

-- Bihar
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Patna', '800001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Patna', '800002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Gaya', '823001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Muzaffarpur', '842001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Bhagalpur', '812001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Darbhanga', '846004');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Bihar', 'Purnia', '854301');

-- Chhattisgarh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chhattisgarh', 'Raipur', '492001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chhattisgarh', 'Bhilai', '490001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chhattisgarh', 'Bilaspur', '495001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chhattisgarh', 'Korba', '495677');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chhattisgarh', 'Durg', '491001');

-- Goa
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Goa', 'Panaji', '403001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Goa', 'Margao', '403601');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Goa', 'Vasco da Gama', '403802');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Goa', 'Mapusa', '403507');

-- Gujarat
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Ahmedabad', '380001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Ahmedabad', '380015');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Surat', '395001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Surat', '395003');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Vadodara', '390001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Rajkot', '360001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Gandhinagar', '382010');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Bhavnagar', '364001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Gujarat', 'Jamnagar', '361001');

-- Haryana
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Gurgaon', '122001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Faridabad', '121001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Panipat', '132103');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Ambala', '134003');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Karnal', '132001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Hisar', '125001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Haryana', 'Rohtak', '124001');

-- Himachal Pradesh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Himachal Pradesh', 'Shimla', '171001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Himachal Pradesh', 'Manali', '175131');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Himachal Pradesh', 'Dharamshala', '176215');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Himachal Pradesh', 'Kullu', '175101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Himachal Pradesh', 'Solan', '173212');

-- Jharkhand
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jharkhand', 'Ranchi', '834001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jharkhand', 'Jamshedpur', '831001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jharkhand', 'Dhanbad', '826001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jharkhand', 'Bokaro', '827001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jharkhand', 'Deoghar', '814112');

-- Karnataka
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Bengaluru', '560001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Bengaluru', '560002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Mysuru', '570001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Hubli', '580020');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Mangaluru', '575001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Belagavi', '590001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Karnataka', 'Davangere', '577001');

-- Kerala
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Kerala', 'Thiruvananthapuram', '695001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Kerala', 'Kochi', '682001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Kerala', 'Kozhikode', '673001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Kerala', 'Thrissur', '680001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Kerala', 'Kannur', '670001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Kerala', 'Alappuzha', '688001');

-- Madhya Pradesh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Madhya Pradesh', 'Bhopal', '462001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Madhya Pradesh', 'Indore', '452001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Madhya Pradesh', 'Jabalpur', '482001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Madhya Pradesh', 'Gwalior', '474001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Madhya Pradesh', 'Ujjain', '456001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Madhya Pradesh', 'Sagar', '470001');

-- Maharashtra
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Mumbai', '400001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Mumbai', '400002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Mumbai', '400050');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Pune', '411001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Pune', '411002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Nagpur', '440001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Nashik', '422001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Aurangabad', '431001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Thane', '400601');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Navi Mumbai', '400703');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Solapur', '413001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Maharashtra', 'Kolhapur', '416001');

-- Manipur
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Manipur', 'Imphal', '795001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Manipur', 'Thoubal', '795138');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Manipur', 'Bishnupur', '795126');

-- Meghalaya
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Meghalaya', 'Shillong', '793001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Meghalaya', 'Tura', '794001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Meghalaya', 'Jowai', '793150');

-- Mizoram
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Mizoram', 'Aizawl', '796001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Mizoram', 'Lunglei', '796701');

-- Nagaland
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Nagaland', 'Kohima', '797001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Nagaland', 'Dimapur', '797112');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Nagaland', 'Mokokchung', '798601');

-- Odisha
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Odisha', 'Bhubaneswar', '751001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Odisha', 'Cuttack', '753001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Odisha', 'Rourkela', '769001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Odisha', 'Berhampur', '760001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Odisha', 'Sambalpur', '768001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Odisha', 'Puri', '752001');

-- Punjab
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Punjab', 'Chandigarh', '160001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Punjab', 'Ludhiana', '141001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Punjab', 'Amritsar', '143001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Punjab', 'Jalandhar', '144001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Punjab', 'Patiala', '147001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Punjab', 'Bathinda', '151001');

-- Rajasthan
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Jaipur', '302001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Jaipur', '302002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Jodhpur', '342001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Udaipur', '313001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Kota', '324001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Ajmer', '305001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Bikaner', '334001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Rajasthan', 'Jaisalmer', '345001');

-- Sikkim
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Sikkim', 'Gangtok', '737101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Sikkim', 'Namchi', '737126');

-- Tamil Nadu
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Chennai', '600001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Chennai', '600002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Coimbatore', '641001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Madurai', '625001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Tiruchirappalli', '620001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Salem', '636001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Tirunelveli', '627001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tamil Nadu', 'Vellore', '632001');

-- Telangana
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Hyderabad', '500001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Hyderabad', '500003');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Warangal', '506001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Nizamabad', '503001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Karimnagar', '505001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Khammam', '507001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Telangana', 'Secunderabad', '500003');

-- Tripura
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tripura', 'Agartala', '799001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Tripura', 'Udaipur', '799120');

-- Uttar Pradesh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Lucknow', '226001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Lucknow', '226002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Noida', '201301');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Kanpur', '208001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Agra', '282001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Varanasi', '221001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Allahabad', '211001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Ghaziabad', '201001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Meerut', '250001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttar Pradesh', 'Bareilly', '243001');

-- Uttarakhand
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttarakhand', 'Dehradun', '248001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttarakhand', 'Haridwar', '249401');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttarakhand', 'Rishikesh', '249201');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttarakhand', 'Nainital', '263001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttarakhand', 'Haldwani', '263139');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Uttarakhand', 'Roorkee', '247667');

-- West Bengal
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Kolkata', '700001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Kolkata', '700002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Howrah', '711101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Darjeeling', '734101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Siliguri', '734001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Durgapur', '713201');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('West Bengal', 'Asansol', '713301');

-- Union Territories
-- Delhi
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'New Delhi', '110001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'New Delhi', '110002');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'New Delhi', '110003');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'North Delhi', '110007');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'South Delhi', '110017');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'East Delhi', '110092');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'West Delhi', '110063');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Delhi', 'Dwarka', '110075');

-- Chandigarh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chandigarh', 'Chandigarh', '160001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Chandigarh', 'Chandigarh', '160017');

-- Puducherry
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Puducherry', 'Puducherry', '605001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Puducherry', 'Karaikal', '609602');

-- Andaman and Nicobar Islands
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Andaman and Nicobar Islands', 'Port Blair', '744101');

-- Dadra and Nagar Haveli and Daman and Diu
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Dadra and Nagar Haveli and Daman and Diu', 'Silvassa', '396230');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Dadra and Nagar Haveli and Daman and Diu', 'Daman', '396210');

-- Lakshadweep
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Lakshadweep', 'Kavaratti', '682555');

-- Ladakh
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Ladakh', 'Leh', '194101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Ladakh', 'Kargil', '194103');

-- Jammu and Kashmir
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jammu and Kashmir', 'Srinagar', '190001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jammu and Kashmir', 'Jammu', '180001');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jammu and Kashmir', 'Anantnag', '192101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jammu and Kashmir', 'Baramulla', '193101');
MERGE INTO location (state, city, pincode) KEY(state, city, pincode) VALUES ('Jammu and Kashmir', 'Kathua', '184101');
