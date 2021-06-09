(ns financial-health-dashboard.domain)

(def data {:user/name             "Bob"
           :user/age              28
           :user/accounts         [{:account/name   "FNB Cheque Account"
                                    :account/type   :bank
                                    :account/id     1
                                    :account/active true}
                                   {:account/name   "Easy Equities TFSA"
                                    :account/type   :investment
                                    :account/id     2
                                    :account/active true}]
           :user/account-balances [{:account/id             1
                                    :account-balance/year   2021
                                    :account-balance/month  6
                                    :account-balance/amount 100}
                                   {:account/id             2
                                    :account-balance/year   2021
                                    :account-balance/month  6
                                    :account-balance/amount 200}
                                   {:account/id             2
                                    :account-balance/year   2021
                                    :account-balance/month  1
                                    :account-balance/amount 400}]})


(defn user-name
  "Return a user's name"
  [{:user/keys [name]}]
  name)

(defn user-age
  "Return a user's age"
  [{:user/keys [age]}]
  age)

(defn user-accounts
  "Return a user's active accounts, filtered if account-type is provided"
  ([{:user/keys [accounts]}]
   (filter #(:account/active %) accounts))
  ([data account-type]
   (filter
     #(= (:account/type %) account-type)
     (user-accounts data))))

(defn account
  "Return an account by provided account-id"
  [data account-id]
  (let [accounts (user-accounts data)]
    (first (filter #(= (:account/id %) account-id) accounts))))

(defn account-balances
  "Returns all account balances, filtered if year or month are provided"
  ([data]
   (:user/account-balances data))

  ([data year]
   (filter #(= (:account-balance/year %) year)
           (account-balances data)))

  ([data year month]
   (filter #(and
              (= (:account-balance/year %) year)
              (= (:account-balance/month %) month))
           (account-balances data))))

(defn balance-exists-for-account?
  "Check if balance exists for account"
  [account-id year month]
  (let [balances             (account-balances data year month)
        balances-for-account (filter #(= (:account/id %) account-id) balances)]
    (pos? (count balances-for-account))))

(defn net-worth
  "Calculate net worth for year and month"
  [data year month]
  (reduce
    (fn [acc
         {:account-balance/keys [amount]}]
      (+ acc amount))
    0
    (account-balances data year month)))


